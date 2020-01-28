package pl.wat.wcy.arapplication

class MockLocationObjectProvider: LocationObjectProvider {

    private val objects = setOf(
        LocationObject("zdarzenie", ObjectType.EVENT, CustomLocation(52.258840, 20.897790)),
        LocationObject("budynek", ObjectType.BUILDING, CustomLocation(52.252174, 20.913394))
    )

    override fun getObjects(): Set<LocationObject> {
        return objects
    }
}