package com.example.manualfocusmacrocamera.ui.camera

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    var diopters: Float = 0f
        private set
    private val context = application
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var imageCapture: ImageCapture

    @OptIn(ExperimentalCamera2Interop::class)
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun setupCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner): Float {
        cameraProvider = context.getCameraProvider()
        val logicalBackCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        // 論理カメラのID。論理カメラは複数の物理カメラにより構成される場合があり、マクロカメラはこのパターンに該当するはず
        // （つまり論理カメラIDをさらに分解してマクロカメラに該当する物理カメラIDを取得する必要がある）
        val backCameraId = camMgr.cameraIdList.firstOrNull {
            val characteristics = camMgr.getCameraCharacteristics(it)
            diopters =
                characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }.orEmpty()

        val capabilities = camMgr.getCameraCharacteristics(backCameraId)
            .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val isLogicalMultiCamera =
            capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true

        val targetCameraId = if (isLogicalMultiCamera) {
            val physicalCameraIds =
                camMgr.getCameraCharacteristics(backCameraId).physicalCameraIds
            // 明示的にマクロカメラを取得する方法はなさそうなので、複数あるカメラのうち最短焦点距離が短いものをマクロカメラと見なすことにしている。
            // LENS_INFO_MINIMUM_FOCUS_DISTANCE で取れる数値はディオプターという単位であり、数値が大きいほど焦点距離が短いそう。
            val consideredTheMostMacroLensIdAndMaximumFocus = physicalCameraIds.map { cameraId ->
                val characteristics = camMgr.getCameraCharacteristics(cameraId)
                val diopters =
                    characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                cameraId to diopters
            }.maxByOrNull { it.second ?: 0f }
            diopters = consideredTheMostMacroLensIdAndMaximumFocus?.second ?: 0f
            consideredTheMostMacroLensIdAndMaximumFocus?.first.orEmpty()
        } else {
            // 背面カメラが複数ではない場合はマクロカメラはないものと考えてもよければここはマクロ非対応のアラートを表示するロジックでいいが、デバイスの統一仕様がわからないので保留。
            backCameraId
        }

        val previewBuilder = Preview.Builder()
        Camera2Interop.Extender(previewBuilder).setPhysicalCameraId(targetCameraId)
        val imageCaptureBuilder = ImageCapture.Builder()
        Camera2Interop.Extender(imageCaptureBuilder).setPhysicalCameraId(targetCameraId)
        imageCapture = imageCaptureBuilder.build()

        cameraProvider!!.unbindAll()
        camera = cameraProvider!!.bindToLifecycle(
            lifecycleOwner,
            logicalBackCameraSelector,
            previewBuilder.build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            },
            imageCapture,
        )

        return diopters
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun setManualFocus(distance: Float) {
        val camera2Control = camera?.let { Camera2CameraControl.from(it.cameraControl) } ?: return
        val options = CaptureRequestOptions.Builder().setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_MACRO
        ).setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF
        ).setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, distance).build()
        camera2Control.captureRequestOptions = options
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun takePhoto(context: Context, onSaved: (Uri) -> Unit, onError: (Exception) -> Unit) {
        if (!cameraProvider?.isBound(imageCapture)!!) {
            onError(IllegalStateException("Camera is not bound"))
            return
        }
        if (!::imageCapture.isInitialized) {
            onError(IllegalStateException("imageCapture not initialized"))
            return
        }

        val resolver = context.contentResolver
        val outputUri: Uri =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = createContentValues()

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            resolver, outputUri, contentValues,
        ).build()

        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onSaved(outputUri)

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(outputUri, contentValues, null, null)
                }

                override fun onError(exc: ImageCaptureException) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(outputUri, contentValues, null, null)

                    onError(exc)
                }
            })
    }

    private fun createContentValues(): ContentValues {
        val photoTitle = "タイトル_${System.currentTimeMillis()}"
        val photoDisplayName = "${System.currentTimeMillis()}撮影.jpg"
        val mimeType = "image/jpeg"

        return ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, photoTitle)
            put(MediaStore.Images.Media.DISPLAY_NAME, photoDisplayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
}


suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(this)
    future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(this))
}