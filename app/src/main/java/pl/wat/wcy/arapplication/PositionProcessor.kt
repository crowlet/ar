package pl.wat.wcy.arapplication

import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.WindowManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.reactivestreams.Publisher
import java.util.*
import java.util.function.Supplier
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class PositionProcessor(val windowManager: WindowManager) {

    private val gyro = FloatArray(3)
    private var gyroMatrix = FloatArray(9)
    private var gyroOrientation = FloatArray(3)
    private val magnet = FloatArray(3)
    private val accel = FloatArray(3)
    private var accMagOrientation = FloatArray(3)
    private var fusedOrientation = FloatArray(3)
    private val rawRotationMatrix = FloatArray(9)
    private val rotationMatrix = FloatArray(9)

    private val EPSILON = 0.000000001f
    private val NS2S = 1.0f / 1000000000.0f
    private var timestamp: Long = 0L
    private var initState = true
    private val filterCoefficient = 0.98f

    private val TIME_CONSTANT = 30L
    private val fuseTimer = Timer()

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private var lastKnownLocation: Optional<Location> = Optional.empty()
    val locationSubject: Subject<PhonePosition> = PublishSubject.create()

    init {
        sensorThread = HandlerThread("Sensor fusion thread", Thread.MAX_PRIORITY)
        sensorThread!!.start()
        sensorHandler = Handler(sensorThread!!.looper)
        fuseTimer.scheduleAtFixedRate(CalculateFusedOrientationTask(), 1000, TIME_CONSTANT)
    }

    fun magneticFieldEvent(event: SensorEvent) {
        System.arraycopy(event.values, 0, magnet, 0, 3)
    }

    fun accelerometerEvent(event: SensorEvent) {
        System.arraycopy(event.values, 0, accel, 0, 3)
        calculateAccMagOrientation()
    }

    fun gyroscopeEvent(event: SensorEvent) {
        processGyroEvent(event)
    }

    fun locationEvent(location: Location?) {
        lastKnownLocation = Optional.ofNullable(location)
    }

    private fun processGyroEvent(event: SensorEvent) {

        if (initState) {
            val initMatrix = getRotationMatrixFromOrientation(accMagOrientation)
            val test = FloatArray(3)
            SensorManager.getOrientation(initMatrix, test)
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix)
            initState = false
        }

        val deltaVector = FloatArray(4)
        if (timestamp != 0L) {
            val dT = (event.timestamp - timestamp) * NS2S
            System.arraycopy(event.values, 0, gyro, 0, 3)
            getRotationVectorFromGyro(gyro, deltaVector, dT.toDouble() / 2)
        }

        timestamp = event.timestamp

        var deltaMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector)
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix)

        SensorManager.getOrientation(gyroMatrix, gyroOrientation)

    }

    private fun getRotationVectorFromGyro(
        gyroValues: FloatArray,
        deltaRotationVector: FloatArray,
        timeFactor: Double
    ) {
        val normValues = FloatArray(3)

        //predkosc katowa
        val omegaMagnitude =
            Math.sqrt((gyroValues[0] * gyroValues[0] + gyroValues[1] * gyroValues[1] + gyroValues[2] * gyroValues[2]).toDouble())
                .toFloat()

        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude
            normValues[1] = gyroValues[1] / omegaMagnitude
            normValues[2] = gyroValues[2] / omegaMagnitude
        }

        val thetaOverTwo = omegaMagnitude * timeFactor.toFloat()
        val sinThetaOverTwo = sin(thetaOverTwo)
        val cosThetaOverTwo = cos(thetaOverTwo)
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0]
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1]
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2]
        deltaRotationVector[3] = cosThetaOverTwo
    }

    private fun calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rawRotationMatrix, null, accel, magnet)) {
            when(windowManager.defaultDisplay.rotation) {
                Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(rawRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, rotationMatrix)
                Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(rawRotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotationMatrix)
                Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(rawRotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, rotationMatrix)
                Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(rawRotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, rotationMatrix)
            }
            SensorManager.getOrientation(rotationMatrix, accMagOrientation)
        }

    }

    private fun getRotationMatrixFromOrientation(accMagOrientation: FloatArray): FloatArray {
        val xM = FloatArray(9)
        val yM = FloatArray(9)
        val zM = FloatArray(9)

        val sinX = sin(accMagOrientation[1].toDouble()).toFloat()
        val cosX = cos(accMagOrientation[1].toDouble()).toFloat()
        val sinY = sin(accMagOrientation[2].toDouble()).toFloat()
        val cosY = cos(accMagOrientation[2].toDouble()).toFloat()
        val sinZ = sin(accMagOrientation[0].toDouble()).toFloat()
        val cosZ = cos(accMagOrientation[0].toDouble()).toFloat()
        xM[0] = 1.0f
        xM[1] = 0.0f
        xM[2] = 0.0f
        xM[3] = 0.0f
        xM[4] = cosX
        xM[5] = sinX
        xM[6] = 0.0f
        xM[7] = -sinX
        xM[8] = cosX

        yM[0] = cosY
        yM[1] = 0.0f
        yM[2] = sinY
        yM[3] = 0.0f
        yM[4] = 1.0f
        yM[5] = 0.0f
        yM[6] = -sinY
        yM[7] = 0.0f
        yM[8] = cosY

        zM[0] = cosZ
        zM[1] = sinZ
        zM[2] = 0.0f
        zM[3] = -sinZ
        zM[4] = cosZ
        zM[5] = 0.0f
        zM[6] = 0.0f
        zM[7] = 0.0f
        zM[8] = 1.0f
        return matrixMultiplication(zM, matrixMultiplication(xM, yM))
    }

    private fun matrixMultiplication(A: FloatArray, B: FloatArray): FloatArray {
        val result = FloatArray(9)

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6]
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7]
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8]

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6]
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7]
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8]

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6]
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7]
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8]

        return result
    }

    inner class CalculateFusedOrientationTask() : TimerTask() {

        private fun sensorFusion(index: Int) {
            val oneMinusCoefficient = 1.0f - filterCoefficient
//            if(gyroOrientation[index] < -0.5* PI && accMagOrientation[index] > 0.0) {
//                fusedOrientation[index] = (filterCoefficient*(gyroOrientation[index] + 2.0* PI) + oneMinusCoefficient*accMagOrientation[index]).toFloat()
//                fusedOrientation[index] -= (if(fusedOrientation[index] > PI) (2.0 * PI).toFloat() else 0.toFloat())
//            } else if (accMagOrientation[index] < -0.5 * PI && gyroOrientation[index] > 0.0) {
//                fusedOrientation[index] = (filterCoefficient*gyroOrientation[0] + oneMinusCoefficient*(accMagOrientation[index] + 2.0 * PI)).toFloat()
//                fusedOrientation[index] -= (if(fusedOrientation[index] > PI) (2.0*PI).toFloat() else 0.0.toFloat())
//            } else {
//                fusedOrientation[index] = filterCoefficient*accMagOrientation[index] + oneMinusCoefficient*fusedOrientation[index]
//            }
            fusedOrientation[index] = filterCoefficient*accMagOrientation[index] + oneMinusCoefficient*fusedOrientation[index]
        }

        override fun run() {
            sensorFusion(0)
            sensorFusion(1)
            sensorFusion(2)

            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation)
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3)
            sensorHandler!!.post(updateOrientationTask)

        }

        private val updateOrientationTask = Runnable { updateOrientation() }

        private fun updateOrientation() {
            lastKnownLocation.ifPresent{l -> broadcastPosition(PhonePosition(
                fusedOrientation[0].toDouble(),
                fusedOrientation[1].toDouble(),
                fusedOrientation[2].toDouble(),
                CustomLocation(l.latitude, l.longitude)
            ))}
        }

    }

    private fun broadcastPosition(position: PhonePosition) {
        locationSubject.onNext(position)
    }
}