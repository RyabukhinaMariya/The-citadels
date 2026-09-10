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

    override fun build (district: IDistrict) {
        gold -= district.cost
        hand.remove(district)
        city.add(district)
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
