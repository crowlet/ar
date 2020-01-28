package pl.wat.wcy.arapplication

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Message
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView

class Camera(val surfaceView: SurfaceView, val cameraManager: CameraManager): SurfaceHolder.Callback, Handler.Callback {
    override fun handleMessage(msg: Message): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var camSurfaceHolder: SurfaceHolder
    private lateinit var cameraList: Array<String>
    private lateinit var cameraCallback: CameraDevice.StateCallback
    private lateinit var cameraDevice: CameraDevice
    private val MSG_CAMERA_OPENED = 1
    private val MSG_SURFACE_READY = 2
    private var surfaceCreated = false
    private var cameraConfigured = false
    private lateinit var cameraSurface: Surface
    private lateinit var captureSession: CameraCaptureSession

    init {
        camSurfaceHolder = surfaceView.holder
        camSurfaceHolder.addCallback(this)
        cameraList = cameraManager.cameraIdList
        cameraCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                cameraMessage(MSG_CAMERA_OPENED)
            }

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }

        }
    }

    private fun cameraMessage(msg: Int) {
        when (msg) {
            MSG_CAMERA_OPENED, MSG_SURFACE_READY -> {
                if (surfaceCreated && (::cameraDevice.isInitialized) && !cameraConfigured) {
                    configureCamera()
                }
            }
        }
    }

    private fun configureCamera() {
        val surfacesList = ArrayList<Surface>(1)
        surfacesList.add(cameraSurface)
        cameraDevice.createCaptureSession(surfacesList, CaptureSessionListener(), null)
        cameraConfigured = true
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        cameraManager.openCamera(cameraList[0], cameraCallback, Handler())
    }

    fun closeCamera() {
        if (::captureSession.isInitialized) {
            captureSession.stopRepeating()
            captureSession.close()
            captureSession
        }
        cameraConfigured = false

        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
    }

    inner class CaptureSessionListener : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
        }

        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            val previewRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(cameraSurface)
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null)
        }

    }
    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        cameraSurface = holder!!.surface
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        surfaceCreated = false
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        cameraSurface = holder!!.surface
        surfaceCreated = true
        cameraMessage(MSG_SURFACE_READY)
    }
}