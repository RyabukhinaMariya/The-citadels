package engine

import domain.GameCharacters.getByOrder
import domain.IDistrict
import domain.IPlayer
import domain.Player
import domain.CitadelConfig
import domain.QuarterPool

interface InputProvider {
    fun readInput(): String
}

class ConsoleInput : InputProvider {
    override fun readInput() = readln()
}

class GameSession() {
    private val quarterPool = QuarterPool(CitadelConfig.getBaseDeck())
    private val players = mutableListOf<IPlayer>()
    private var isGameOver = false

    fun getStartCards(player: IPlayer) {
        for (i in 1..4) {
            player.addToHand(quarterPool.drawCard())
        }
    }

    fun setupGame() {
        println("=== Введите количество игроков (4-7): ===")
        val playerCount = run {
            while (true) {
                val input = ConsoleInput().readInput().toIntOrNull()
                if (input != null && input in 4..7) return@run input
                println("\nВведите корректное число (4-7): ")
            }
        }

        println("\n=== Регистрация игроков ===")
        for (i in 1..playerCount as Int) {
            while (true) {
                print("\nВведите имя для игрока №$i: ")
                val name = ConsoleInput().readInput()
                if (name.isNotBlank()) {
                    if (players.none { it.name.equals(name, ignoreCase = true) }) {
                        val player = Player(name = name, id = i)
                        players.add(player)
                        getStartCards(player)
                        break
                    } else println("\nЭто имя уже занято.")
                } else println("\nИмя не может быть пустым.")
            }
        }

        println("\nИгра создана! Участники: ${players.joinToString { it.name }}")
        gameLoop(false)
    }

    fun gameLoop(isGameOver: Boolean) {
        while (!isGameOver) {
            println("\n--- Начало нового раунда ---")
            for (i in 1..8) {
                getByOrder(i)?.isKilled = false
            }
            playRound()
        }
        showFinalResults()
    }

    fun playRound() {
        println("\n--- Раздайте карты и выберите персонажей ---")

        for (i in 1..8) {
            val character = getByOrder(i) ?: continue

            if (character.isKilled) {
                println("\nПерсонаж ${character.name} убит и пропускает раунд.")
                continue
            }

            println("\n--- Ход персонажа: ${character.name} ---")

            println("Персонаж ${character.name} в игре? Введите его ник, или N:")
            val inRound = run {
                while (true) {
                    val input = ConsoleInput().readInput()
                    if ((players.find { it.name.equals(input, ignoreCase = true) } != null) || input == "N") {
                        return@run input
                    }

                    println("\nВведите корректный ответ (ник или N):")
                }
            }

            if (inRound != "N") {
                val player = players.find { it.name.equals(inRound as String?, ignoreCase = true) } ?: error("Игрок $inRound не найден\n")
                character.ability(player, players, quarterPool)
                println("Игрок берет монеты (M) или карты (C)?:")
                if (ConsoleInput().readInput() == "M") {
                    player.gold += 2
                    println("${player.name} получил 2 золотых (Всего: ${player.gold})")
                } else {
                    val drawn = quarterPool.drawCard()
                    player.addToHand(drawn)
                    println("${player.name} вытянул карту: ${drawn.name}")
                }

                println("Игрок строит здание?(Y/N)")
                if (ConsoleInput().readInput() == "Y") {
                    val card = run {
                        while (true) {
                            val input = ConsoleInput().readInput()
                            if ((player.hand.find { it.name.equals(input, ignoreCase = true) } != null) || input == "N") {
                                val cardName = quarterPool.findDistrictCard(input)
                                return@run cardName
                            }

                            println("\nВведите корректный ответ (ник или N):")
                        }
                    }
                    player.build(card as IDistrict)
                }

                if (player.city.size >= 7) {
                    isGameOver = true
                }
            }
        }
    }

    private fun showFinalResults() {
        println("\n=== ИГРА ОКОНЧЕНА. РЕЗУЛЬТАТЫ: ===")
        players.sortedByDescending { it.city.sumOf { district -> district.cost } }
            .forEach { player ->
                val score = player.city.sumOf { it.cost }
                println("Игрок ${player.name}: $score очков")
            }
    }
}