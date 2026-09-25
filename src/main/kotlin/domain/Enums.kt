package domain

import models.Architect
import models.Assassin
import models.Battlefield
import models.Bishop
import models.Castle
import models.Cathedral
import models.Church
import models.Docks
import models.Fortress
import models.Harbor
import models.King
import models.Manor
import models.Market
import models.Merchant
import models.Monastery
import models.Palace
import models.Prison
import models.Shop
import models.Sorcerer
import models.Tavern
import models.Temple
import models.Thief
import models.TownHall
import models.Warlord
import models.Watchtower

enum class QuarterColor {
    YELLOW,   // Дворянский (доход Королю)
    BLUE,     // Церковный (доход Епископу)
    GREEN,    // Торговый (доход Купцу)
    RED,      // Военный (доход Кондотьеру)
}

object GameCharacters {
    val all = listOf(Assassin(), Thief(), Sorcerer(), King(), Bishop(), Merchant(), Architect(), Warlord())

    fun getByOrder(rank: Int): ICharacter? {
        return all.find { it.rank == rank }
    }
}

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
            Tavern() to 20,
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
