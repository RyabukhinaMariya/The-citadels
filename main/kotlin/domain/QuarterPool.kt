package domain

import engine.ConsoleInput

class QuarterPool(initialDistricts: Map<IDistrict, Int>) {
    val districtCards = initialDistricts.toMutableMap()

    fun findDistrictCard(cardName: String?): IDistrict? {
        val district = districtCards.keys.find { it.name == cardName }
        if (district != null && ((districtCards[district] ?: 0) > 0)) {
            return district
        }
        return null
    }

    fun drawCard(): IDistrict {
        println("\nВведите название вытянутой карты")
        val district = run {
            while (true) {
                val input = ConsoleInput().readInput()
                val found = findDistrictCard(input)
                if (found != null) {
                    return@run found
                }

                println("\nВведите корректное название карты")
            }
        }

        val currentCount = districtCards[district] ?: 0
        districtCards[district as IDistrict]  = currentCount - 1
        return district
    }

    // возвращение карты в колоду
    fun discardCard(player: IPlayer?): IDistrict {
        println("\nВведите название сбрасываемой карты")
        val districtCard = run {
            while (true) {
                val input = ConsoleInput().readInput()
                val districtCard = player?.hand?.find {it.name.equals(input, true)}
                if (districtCard != null) {
                    return@run districtCard
                }

                println("\nВведите корректное название карты")
            }
        }

        val currentCount = districtCards[districtCard] ?: 0
        districtCards[districtCard as IDistrict]  = currentCount + 1
        return districtCard
    }
}