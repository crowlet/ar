package pl.wat.wcy.arapplication

import kotlin.math.*


interface LocationObjectProvider {
    companion object {
        private val earthRadius = 6371
    }

    fun getObjects(): Set<LocationObject>

    fun toRad(degree: Double): Double {
        return degree * PI / 180
    }

    fun toDegree(degree: Double): Double {
        return degree * 180 / PI
    }



    fun calculateAzimuth(phoneLocation: CustomLocation, objectLocation: CustomLocation): Double {
        val fi1 = toRad(phoneLocation.latitude)
        val fi2 = toRad(objectLocation.latitude)
        val deltaFi = toRad(objectLocation.latitude - phoneLocation.latitude)
        val deltaLambda = toRad(objectLocation.longitude - phoneLocation.longitude)
        val a =
            sin(deltaFi / 2) * sin(deltaFi / 2) + cos(fi1) * cos(fi2) * sin(deltaLambda / 2) * sin(
                deltaLambda / 2
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val d = earthRadius * c
        return toDegree(
            atan2(
                sin(deltaLambda) * cos(fi2),
                cos(fi1) * sin(fi2) - sin(fi1) * cos(fi2) * cos(deltaLambda)
            )
        )
    }
}