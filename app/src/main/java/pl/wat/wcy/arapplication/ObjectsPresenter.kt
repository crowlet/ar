package pl.wat.wcy.arapplication

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

class ObjectsPresenter(val parentLayout: FrameLayout, val locationObjectProvider: LocationObjectProvider) {

    private val fov = 77f/2f

    fun phonePositionChanged(phonePosition: PhonePosition) {
        println(phonePosition)
        parentLayout.removeAllViews()
        locationObjectProvider.getObjects().forEach{locationObject -> draw(locationObject, phonePosition)}
    }

    fun draw(locationObject: LocationObject, phonePosition: PhonePosition) {
        val phoneLocation = phonePosition.location
        val objectLocation = locationObject.location
        var phoneAzimuthDegrees = locationObjectProvider.toDegree(phonePosition.azimuth)
        //phoneAzimuthDegrees %= 360
        var azimuth = phoneAzimuthDegrees + locationObjectProvider.calculateAzimuth(phoneLocation, objectLocation)
        //azimuth %= 360
        if(azimuth >= - fov && azimuth <= fov) {
            val imageView = ImageView(parentLayout.context)
            imageView.setImageDrawable(parentLayout.context.getDrawable(locationObject.type.drawable))
            imageView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            imageView.x = centerX(imageView) - (azimuth * (parentLayout.width/2)/fov).toFloat()
            imageView.y = centerY(imageView)
            parentLayout.addView(imageView)
        }
    }

    private fun centerY(view: View) = parentLayout.height / 2f - view.height / 2

    private fun centerX(view: View) = parentLayout.width / 2f - view.width / 2



}