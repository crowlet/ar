package pl.wat.wcy.arapplication

import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.WindowManager
import io.reactivex.Observable

class SensorListener(val sensorManager: SensorManager, val locationManager: LocationManager, val windowManager: WindowManager) :
    SensorEventListener, LocationListener {

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private lateinit var positionProcessor: PositionProcessor

    val location: Observable<PhonePosition> get() = positionProcessor.locationSubject


    @SuppressLint("MissingPermission")
    fun registerListeners() {
        sensorThread = HandlerThread("Sensor thread", Thread.MAX_PRIORITY)
        sensorThread!!.start()
        sensorHandler = Handler(sensorThread!!.looper)
        positionProcessor = PositionProcessor(windowManager)
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST,
            sensorHandler
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST,
            sensorHandler
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_FASTEST,
            sensorHandler
        )
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            5000L,
            10.toFloat(),
            this
        )
        positionProcessor.locationEvent(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
    }

    fun unregisterListeners() {
        sensorManager.unregisterListener(this)
        sensorThread!!.quitSafely()
        sensorThread = null
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> positionProcessor.accelerometerEvent(event)
            Sensor.TYPE_GYROSCOPE -> positionProcessor.gyroscopeEvent(event)
            Sensor.TYPE_MAGNETIC_FIELD -> positionProcessor.magneticFieldEvent(event)
        }
    }
    override fun onLocationChanged(location: Location?) {
        positionProcessor.locationEvent(location)
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }
}