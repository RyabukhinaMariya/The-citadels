package models

import domain.GameCharacters.getByOrder
import domain.ICharacter
import domain.IDistrict
import domain.IPlayer
import domain.Player
import domain.QuarterColor
import engine.ConsoleInput
import engine.QuarterPool
import kotlin.collections.get
import kotlin.collections.set

class Assassin : ICharacter {
    override val name: String = "Ассасин"
    override val rank: Int = 1
    override var isKilled: Boolean = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool,
    ) {
        println("Кого убивает Ассасин?")
        println("\nВведите номер персонажа, если он в игре(2-8), '0' иначе:")
        val targetRank = run{
            while (true) {
                val input = ConsoleInput().readInput().toIntOrNull()

                if (input != null && (input in 2..8 || input == 0)) {
                    return@run input
                }

                println("\nВведите корректный номер (2-8) или '0' если персонаж не в игре:")
            }
        }
        val victim: ICharacter? = getByOrder(targetRank as Int)
        victim?.isKilled = true
        println("Персонаж ${victim?.name} убит и пропустит этот раунд.")
    }
}

class Thief : ICharacter {
    override val name: String = "Вор"
    override val rank: Int = 2
    override var isKilled: Boolean = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        println("\nВор, кого вы хотите ограбить?")
        println("\nВведите его номер (2-8, вы не можете ограбить убитого ассасином):")
        val targetRank = run {
            while (true) {
                val input = ConsoleInput().readInput().toIntOrNull()
                if (input in 2..8) {
                    val victim: ICharacter? = getByOrder(input as Int)
                    if (victim?.isKilled == false) {
                        return@run input
                    }
                }

                println("\nВведите корректный номер (2-8, вы не можете ограбить убитого ассасином):")
            }
        }
        val victim: ICharacter? = getByOrder(targetRank as Int)

        println("\nЕсли персонаж ${victim?.name} в этом раунде введите ник игрока, который играет за него, иначе 'N':")
        val isInRound = ConsoleInput().readInput()
        if (isInRound != "N") {
            val victim: Player = allPlayers.find { it.name == isInRound } as Player
            currentPlayer?.gold += victim.gold
            victim.gold = 0

            println("Игроку ${currentPlayer} добавлено ${victim.gold} золота")
        }
    }

}

class Sorcerer : ICharacter {
    override val name = "Чародей"
    override val rank = 3
    override var isKilled = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        println("\n1. Обменять все свои карты с другим игроком")
        println("\n2. Сбросить карты и взять новые из колоды")
        val choice = run{
            while (true) {
                val input = ConsoleInput().readInput()

                if (input == "1" || input == "2") {
                    return@run input
                }

                println("\nВведите корректный вариант (1/2):")
            }
        }
        if (choice == "1") {
            println("\nВведите ник игрока для обмена:")
            val targetName = run {
                while (true) {
                    val input = ConsoleInput().readInput()

                    if (allPlayers.find { it.name.equals(input, true) } != null) {
                        return@run input
                    }

                    println("\nВведите правильный ник игрока для обмена:")
                }
            }
            val targetPlayer = allPlayers.find { it.name.equals(targetName) }

            val hand1 = currentPlayer?.hand
            val hand2 = targetPlayer?.hand

            if (hand2 != null) {
                currentPlayer?.replaceHand(hand2)
            }
            if (hand1 != null) {
                targetPlayer?.replaceHand(hand1)
            }
        }
        else {
            println("\nСколько карт вы сбрасываете?")
            val count = run {
                while (true) {
                    val input = ConsoleInput().readInput().toIntOrNull()

                    if (input != null && (input <= (currentPlayer?.hand?.size ?: 0))) {
                        return@run input
                    }

                    println("\nВведите корректное количество карт:")
                }
            }

            //сброс карт
            for (i in 0..count as Int) {
                val card = pool.discardCard(currentPlayer)
                currentPlayer?.discardFromHand(card)
            }

            // добавление карт
            for (i in 0..count) {
                val card = pool.drawCard()
                currentPlayer?.addToHand(card)
            }
        }
    }
}

class King : ICharacter {
    override val name = "Король"
    override val rank = 4
    override var isKilled = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        val bonus = currentPlayer?.city?.count { it.color == QuarterColor.YELLOW }
        currentPlayer?.gold += bonus as Int
        println("Король получает $bonus золота за свои владения. Корона переходит к вам!")
    }
}

class Bishop : ICharacter {
    override val name = "Епископ"
    override val rank = 5
    override var isKilled = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        val bonus = currentPlayer?.city?.count { it.color == QuarterColor.BLUE }
        currentPlayer?.gold += bonus as Int
        println("Епископ получил $bonus золота. Кондотьер не может разрушать ваши здания!")
    }
}

class Merchant : ICharacter {
    override val name = "Купец"
    override val rank = 6
    override var isKilled = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        val bonus = currentPlayer?.city?.count { it.color == QuarterColor.GREEN }
        currentPlayer?.gold += (bonus as Int + 1)
        println("Купец получает ${bonus as Int + 1} золота (бонус роли + кварталы).")
    }
}

class Architect : ICharacter {
    override val name = "Архитектор"
    override val rank = 7
    override var isKilled = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        for (i in 1..2) {
            currentPlayer?.addToHand(pool.drawCard())
        }
        println("Архитектор берет 2 дополнительные карты. Вы можете построить до 3-х кварталов!")
    }
}

class Warlord : ICharacter {
    override val name = "Кондотьер"
    override val rank = 8
    override var isKilled = false

    override fun ability(
        currentPlayer: IPlayer?,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    ) {
        val bonus = currentPlayer?.city?.count { it.color == QuarterColor.RED }
        currentPlayer?.gold += bonus as Int

        println("\nХотите разрушить здание? (Y/N)")
        val answ = run {
            while (true) {
                val input = ConsoleInput().readInput()

                if (input == "Y" || input == "N") {
                    return@run input
                }

                println("\nВведите корректный ответ (Y/N):")
            }
        }

        if (answ== "Y") {
            println("\nВведите имя жертвы:")
            val targetName = run {
                while (true) {
                    val input = ConsoleInput().readInput()

                    if (allPlayers.find { it.name.equals(input, true) && input != currentPlayer?.name}!= null) {
                        return@run input
                    }

                    println("\nВведите корректный ник жертвы:")
                }
            }
            val targetPlayer = allPlayers.find { it.name.equals(targetName as String?, true) }

            println("Выберите здание из города который хотите уничтожить:")
            val targetDistrict = run {
                while (true) {
                    val input = ConsoleInput().readInput()
                    val card = currentPlayer?.city?.find { it.name.equals(input, true) }
                    if (card != null && (card.cost <= currentPlayer.gold)) {
                        return@run input
                    }

                    println("\nВведите корректное название карты:")
                }
            }
            targetPlayer?.discardFromHand(targetDistrict as IDistrict)
            val currentCount = pool.districtCards[targetDistrict] ?: 0
            pool.districtCards[targetDistrict as IDistrict]  = currentCount + 1
            currentPlayer?.gold -= targetDistrict.cost
        }
    }
}
