package domain

import engine.ConsoleInput

interface IPlayer: ICard {
    val id: Int
    var gold: Int
    val hand: List<IDistrict>
    val city: List<IDistrict>

    fun canAfford(district: IDistrict): Boolean
    fun discardFromHand(district: IDistrict)
    fun destroyBuilding(district: IDistrict)
    fun addToHand(district: IDistrict)
    fun replaceHand(list: List<IDistrict>)
    fun build()
}

class Player(override val name: String, override val id: Int) : IPlayer {
    override var gold: Int = 2
    override val hand = mutableListOf<IDistrict>()
    override val city = mutableListOf<IDistrict>()

    override fun canAfford(district: IDistrict): Boolean {
        return gold >= district.cost
    }

    override fun build () {
        println("\nВведите название карты, которую хотите построить:")
        val districtCard = run {
            while (true) {
                val input = ConsoleInput().readInput()
                val districtCard = hand.find { it.name.equals(input, true) }
                if (districtCard != null && (districtCard.cost <= gold)) {
                    return@run districtCard
                }

                println("\nВведите корректное название карты")
            }
        }

        gold -= (districtCard as IDistrict).cost
        hand.remove(districtCard)
        city.add(districtCard)
    }

    override fun discardFromHand(district: IDistrict) {
        hand.remove(district)
    }

    override fun destroyBuilding(district: IDistrict) {
        city.remove(district)
    }

    override fun addToHand(district: IDistrict) {
        hand.add(district)
    }

    override fun replaceHand(list: List<IDistrict>) {
        hand.clear()
        for (city in list) {
            addToHand(city)
        }
    }
}
