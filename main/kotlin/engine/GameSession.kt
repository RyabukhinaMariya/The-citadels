package engine

import domain.GameCharacters.getByOrder
import domain.IDistrict
import domain.IPlayer
import domain.Player
import domain.CitadelConfig
import domain.QuarterPool
import domain.ICharacter


class ConsoleInput {
    fun readInput() = readln()
}

class GameSession(
) {
    private val input = ConsoleInput()
    private val quarterPool = QuarterPool(CitadelConfig.getBaseDeck())
    private val players = mutableListOf<IPlayer>()
    private var isGameOver = false


    fun setupGame() {
        val playerCount = askPlayerCount()
        registerPlayers(playerCount)
        println("\nИгра создана! Участники: ${players.joinToString { it.name }}")
        gameLoop()
        }

        private fun askPlayerCount(): Int {
            println("=== Введите количество игроков (4-7): ===")
            return readUntil("\nВведите корректное число (4-7): ") { input ->
                input.toIntOrNull()?.takeIf { it in 4..7 }
            }
        }

        private fun registerPlayers(count: Int) {
            println("\n=== Регистрация игроков ===")
            for (i in 1..count) {
                val name = askUniquePlayerName(i)
                val player = Player(id = i)
                players.add(player)
                dealStartCards(player)
            }
        }

        private fun askUniquePlayerName(index: Int): String {
            while (true) {
                print("\nВведите имя для игрока №$index: ")
                val name = input.readInput()
                when {
                    name.isBlank() -> println("\nИмя не может быть пустым.")
                    players.any { it.name.equals(name, ignoreCase = true) } ->
                    println("\nЭто имя уже занято.")
                    else -> return name
                }
            }
        }

        private fun dealStartCards(player: IPlayer) {
            repeat(4) { player.addToHand(quarterPool.drawCard()) }
        }


        fun gameLoop() {
            while (!isGameOver) {
                startNewRound()
                playRound()
            }
            showFinalResults()
        }

        private fun startNewRound() {
            println("\n--- Начало нового раунда ---")
            for (i in 1..8) {
                getByOrder(i)?.isKilled = false
            }
        }

        fun playRound() {
            println("\n--- Раздайте карты и выберите персонажей ---")
            for (rank in 1..8) {
                val character = getByOrder(rank) ?: continue
                playCharacterTurn(character)
                if (isGameOver) return
            }
        }

        private fun playCharacterTurn(character: ICharacter) {
            if (character.isKilled) {
                println("\nПерсонаж ${character.name} убит и пропускает раунд.")
                return
            }

            println("\n--- Ход персонажа: ${character.name} ---")
            val player = askPlayerForCharacter(character) ?: return

            character.ability(player, players, quarterPool)
            giveCoinsOrCards(player)
            offerBuild(player)
            checkGameOver(player)
        }

        private fun askPlayerForCharacter(character: ICharacter): IPlayer? {
            println("Персонаж ${character.name} в игре? Введите его ник, или N:")
            val answer = readUntil("\nВведите корректный ответ (ник или N):") { raw ->
                when {
                    raw == "N" -> raw
                    players.any { it.name.equals(raw, ignoreCase = true) } -> raw
                    else -> null
                }
            }
            if (answer == "N") return null
                return players.first { it.name.equals(answer, ignoreCase = true) }
        }

        private fun giveCoinsOrCards(player: IPlayer) {
            println("Игрок берет монеты (M) или карты (C)?:")
            val choice = readUntil("\nВведите M или C:") { it.takeIf { c -> c == "M" || c == "C" } }

            if (choice == "M") {
                player.gold += 2
                println("${player.name} получил 2 золотых (Всего: ${player.gold})")
            } else {
                val drawn = quarterPool.drawCard()
                player.addToHand(drawn)
                println("${player.name} вытянул карту: ${drawn.name}")
            }
        }

        private fun offerBuild(player: IPlayer) {
            println("Игрок строит здание?(Y/N)")
            val answer = readUntil("\nВведите Y или N:") { it.takeIf { c -> c == "Y" || c == "N" } }
            if (answer != "Y") return

                val district = askDistrictToBuild(player) ?: return
                player.build(district)
        }

        private fun askDistrictToBuild(player: IPlayer): IDistrict? {
            println("\nВведите название карты из руки для постройки, или N:")
            val cardName = readUntil("\nВведите корректное название карты или N:") { raw ->
                when {
                    raw == "N" -> raw
                    player.hand.any { it.name.equals(raw, ignoreCase = true) } -> raw
                    else -> null
                }
            }
            if (cardName == "N") return null
                return player.hand.first { it.name.equals(cardName, ignoreCase = true) }
        }

        private fun checkGameOver(player: IPlayer) {
            if (player.city.size >= 7) {
                isGameOver = true
            }
        }


        private fun showFinalResults() {
            println("\n=== ИГРА ОКОНЧЕНА. РЕЗУЛЬТАТЫ: ===")
            players
            .sortedByDescending { it.city.sumOf { d -> d.cost } }
            .forEach { player ->
                val score = player.city.sumOf { it.cost }
                println("Игрок ${player.name}: $score очков")
            }
        }

        private fun <T> readUntil(errorMessage: String, parse: (String) -> T?): T {
            while (true) {
                val parsed = parse(input.readInput())
                if (parsed != null) return parsed
                    println(errorMessage)
            }
        }
    }
