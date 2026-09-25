package models

import domain.ICharacter
import domain.IPlayer
import domain.Player
import domain.QuarterColor
import engine.QuarterPool

class Assassin : ICharacter {
    override val name: String = "Ассасин"
    override val rank: Int = 1
    override var isKilled: Boolean = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
    }
}

class Thief : ICharacter {
    override val name: String = "Вор"
    override val rank: Int = 2
    override var isKilled: Boolean = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
    }
}

class Sorcerer : ICharacter {
    override val name = "Чародей"
    override val rank = 3
    override var isKilled = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
    }
}

class King : ICharacter {
    override val name = "Король"
    override val rank = 4
    override var isKilled = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
        val bonus = activePlayer?.city?.count { it.color == QuarterColor.YELLOW } ?: 0
        activePlayer?.gold = (activePlayer.gold) + bonus
    }
}

class Bishop : ICharacter {
    override val name = "Епископ"
    override val rank = 5
    override var isKilled = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
        val bonus = activePlayer?.city?.count { it.color == QuarterColor.BLUE } ?: 0
        activePlayer?.gold = (activePlayer.gold) + bonus
    }
}

class Merchant : ICharacter {
    override val name = "Купец"
    override val rank = 6
    override var isKilled = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
        val bonus =activePlayer?.city?.count { it.color == QuarterColor.GREEN } ?: 0
        activePlayer?.gold = (activePlayer.gold) + bonus + 1
    }
}

class Architect : ICharacter {
    override val name = "Архитектор"
    override val rank = 7
    override var isKilled = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
        for (i in 1..2) {
            val availableCards = quarterPool.getAvailableCards()
            if (availableCards.isNotEmpty()) {
                val card = availableCards.random()
                activePlayer?.addToHand(card)
                quarterPool.drawCard(card)
            }
        }
    }
}

class Warlord : ICharacter {
    override val name = "Кондотьер"
    override val rank = 8
    override var isKilled = false

    override fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool) {
        val bonus = activePlayer?.city?.count { it.color == QuarterColor.RED } ?: 0
        activePlayer?.gold = activePlayer.gold + bonus
    }
}