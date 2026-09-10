package domain

import models.Architect
import models.Assassin
import models.Bishop
import models.King
import models.Merchant
import models.Sorcerer
import models.Thief
import models.Warlord

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
