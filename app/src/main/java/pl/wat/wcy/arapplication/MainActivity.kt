package pl.wat.wcy.arapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_ID = 123
    private val permissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    )

    private lateinit var sensorListener: SensorListener
    private lateinit var camera: Camera
    private lateinit var objectsPresenter: ObjectsPresenter
    private lateinit var locationSubscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if(permissions.any(::hasNoPermission)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_ID)
        } else {
            initializeSensorListener()
            initializeCamera()
        }
        objectsPresenter = ObjectsPresenter(objectsContainer, MockLocationObjectProvider())
    }

    override fun onStart() {
        super.onStart()
        camera.openCamera()
        sensorListener.registerListeners()
        locationSubscription = sensorListener.location.throttleLast (100, TimeUnit.MILLISECONDS) .subscribe{ location ->
            runOnUiThread {
                azimuth.setText("Azymut: "  + location.azimuth)
                pitch.setText("Pitch: "  + location.pitch)
                roll.setText("Roll: "  + location.roll)
                objectsPresenter.phonePositionChanged(location)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        camera.closeCamera()
        sensorListener.unregisterListeners()
        locationSubscription.dispose()
    }

    private fun hasNoPermission(p: String): Boolean =
        ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSIONS_ID) {
            var result = true
            permissions.forEachIndexed{index, permission ->
                result = result && (grantResults[index] == PackageManager.PERMISSION_GRANTED)
            }
            if(result) {
                initializeSensorListener()
                initializeCamera()
            }
        }
    }

    private fun initializeCamera() {
        camera = Camera(camSurfaceView, this.getSystemService(Context.CAMERA_SERVICE) as CameraManager)
    }

    private fun initializeSensorListener() {
        sensorListener = SensorListener(
            getSystemService(Context.SENSOR_SERVICE) as SensorManager,
            getSystemService(Context.LOCATION_SERVICE) as LocationManager,
            windowManager
        )

    }
}
