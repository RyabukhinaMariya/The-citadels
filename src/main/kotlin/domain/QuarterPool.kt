package domain

class QuarterPool(initialDistricts: Map<IDistrict, Int>) {
    val districtCards = initialDistricts.toMutableMap()

    fun getAvailableCards(): List<IDistrict> {
        return districtCards.filter { it.value > 0 }.keys.toList()
    }

    fun drawCard(district: IDistrict) {
        val currentCount = districtCards[district] ?: 0
        if (currentCount > 0) {
            districtCards[district] = currentCount - 1
        }
    }

    fun discardCard(district: IDistrict) {
        val currentCount = districtCards[district] ?: 0
        districtCards[district] = currentCount + 1
    }
}