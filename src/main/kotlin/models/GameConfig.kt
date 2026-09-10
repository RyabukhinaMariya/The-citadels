package models
import domain.IDistrict

object CitadelConfig {
    fun getBaseDeck(): Map<IDistrict, Int> {
        return mapOf(
            Watchtower() to 3,
            Prison() to 3,
            Battlefield() to 3,
            Fortress() to 2,
            Manor() to 5,
            Castle() to 4,
            Palace() to 3,
            Tavern() to 5,
            Market() to 4,
            Shop() to 3,
            Docks() to 3,
            Harbor() to 3,
            TownHall() to 2,
            Temple() to 3,
            Church() to 3,
            Monastery() to 3,
            Cathedral() to 2,
        )
    }
}
