package engine

import database.DatabaseManager
import database.DatabaseManager.addResults
import domain.GameCharacters.getByOrder
import domain.IDistrict
import domain.IPlayer
import domain.Player
import domain.CitadelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import domain.QuarterPool
import data.GameLogEntry
import data.MoveType
import domain.ICharacter

//phases of the game
enum class GamePhase {
    SETUP_PLAYERS,
    ROUND_START,
    CHARACTER_CALL,
    SPECIAL_ABILITY,
    ACTION_CHOICE,
    BUILD_CHOICE,
    SELECT_CARD,
    GAME_OVER,
    LEADERBOARD
}

sealed class AbilityDialogState {
    object None : AbilityDialogState()
}

data class GameState(
    val phase: GamePhase = GamePhase.SETUP_PLAYERS,
    val players: List<IPlayer> = emptyList(),
    val currentCharacterIndex: Int = 1,
    val activePlayer: IPlayer? = null,
    val activeCharacterName: String = "",
    val message: String = "",
    val abilityDialog: AbilityDialogState = AbilityDialogState.None
)

class GameSession {
    private val quarterPool = QuarterPool(CitadelConfig.getBaseDeck())
    private val _players = mutableListOf<IPlayer>()
    private var isGameOverFlag = false
    private var thiefTargetRank: Int? = null
    private var thiefName: String? = null

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    fun addPlayer(name: String) {
        if (_gameState.value.phase != GamePhase.SETUP_PLAYERS) return
        if (name.isBlank() || _players.any { it.name.equals(name, ignoreCase = true) }) {
            updateMessage("Имя пустое или уже занято!")
            return
        }
        if (_players.size >= 7) {
            updateMessage("Максимум 7 игроков.")
            return
        }

        val player = Player(name = name, id = _players.size + 1)
        _players.add(player)
        DatabaseManager.registerOrGetPlayer(name)

        val available = quarterPool.getAvailableCards()
        for (i in 0..3) {
            if (available.size > i) {
                val card = available[i]
                player.addToHand(card)
                quarterPool.drawCard(card)
            }
        }

        _gameState.update { it.copy(players = _players.toList(), message = "Игрок $name добавлен.") }
        logMove(MoveType.PLAYER_ADDED, "Игрок $name добавлен в игру", playerName = name)
    }

    fun startGame() {
        if (_players.size < 4) {
            updateMessage("Нужно минимум 4 игрока для старта.")
            return
        }

        resetCharactersState()
        _gameState.update { it.copy(phase = GamePhase.ROUND_START, currentCharacterIndex = 1) }
        logMove(MoveType.GAME_STARTED, "Игра началась (${_players.size} игроков)", playerName = null)
        round()
    }

    private fun resetCharactersState() {
        domain.GameCharacters.all.forEach { it.isKilled = false }
    }

    //round

    private fun round() {
        if (isGameOverFlag) {
            finishGame()
            return
        }

        val (nextIndex, nextCharacter) = findNextAliveCharacter(_gameState.value.currentCharacterIndex)

        if (nextCharacter == null) {
            startNewRound()
            return
        }

        announceCharacter(nextIndex, nextCharacter)
    }

    private fun findNextAliveCharacter(startIndex: Int): Pair<Int, ICharacter?> {
        for (index in startIndex..MAX_CHARACTER_RANK) {
            val character = getByOrder(index)
            if (character != null && !character.isKilled) {
                return index to character
            }
        }
        return MAX_CHARACTER_RANK + 1 to null
    }

    private fun startNewRound() {
        resetCharactersState()
        currentRound++
        _gameState.update { it.copy(currentCharacterIndex = 1, phase = GamePhase.ROUND_START) }
        round()
    }

    private fun announceCharacter(index: Int, character: ICharacter) {
        _gameState.update {
            it.copy(
                phase = GamePhase.CHARACTER_CALL,
                currentCharacterIndex = index,
                activeCharacterName = character.name,
                activePlayer = null,
                message = "Персонаж ${character.name}. Кто за него играет?"
            )
        }
        logMove(
            MoveType.CHARACTER_CALLED,
            "Вызван персонаж: ${character.name}",
            characterName = character.name,
            playerName = null
        )
    }

    companion object {
        private const val MAX_CHARACTER_RANK = 8
    }

    fun confirmCharacterPlayer(playerId: Int?) {
        if (_gameState.value.phase != GamePhase.CHARACTER_CALL) return

        if (playerId == null) {
            handleCharacterSkipped()
            return
        }

        val player = _players.find { it.id == playerId } ?: run {
            updateMessage("Игрок не найден.")
            return
        }

        applyThiefRobberyIfNeeded(player)
        moveToActionChoice(player)
    }

    private fun handleCharacterSkipped() {
        logMove(
            MoveType.CHARACTER_SKIPPED,
            "Персонаж ${_gameState.value.activeCharacterName} никем не взят",
            playerName = null
        )
        _gameState.update { it.copy(currentCharacterIndex = it.currentCharacterIndex + 1) }
        round()
    }

    private fun applyThiefRobberyIfNeeded(victim: IPlayer) {
        val currentRank = _gameState.value.currentCharacterIndex
        if (currentRank != thiefTargetRank) return

        val thief = _players.find { it.name == thiefName }
        val stolen = victim.gold
        victim.gold = 0
        thief?.let { it.gold += stolen }

        updateMessage("Вор ограбил ${victim.name} на $stolen золотых!")
        logMove(
            MoveType.PLAYER_ROBBED,
            "Вор ($thiefName) ограбил ${victim.name} на $stolen золотых",
            characterName = "Вор",
            playerName = thiefName
        )
        thiefTargetRank = null
    }

    private fun moveToActionChoice(player: IPlayer) {
        _gameState.update {
            it.copy(
                phase = GamePhase.ACTION_CHOICE,
                activePlayer = player,
                message = "${player.name}, возьмите 2 золотых или вытяните карту."
            )
        }
    }

    fun skipAbility() {
        val activePlayer = _gameState.value.activePlayer
        updateMessage("Игрок ${activePlayer?.name} пропустил использование способности.")

        _gameState.update { currentState ->
            currentState.copy(
                phase = GamePhase.BUILD_CHOICE
            )
        }
    }

    fun takeGold() {
        val player = _gameState.value.activePlayer ?: return
        player.gold += 2
        logMove(MoveType.GOLD_TAKEN, "${player.name} взял 2 золотых")

        moveToBuildChoice(
            message = "${player.name} получил 2 золотых. Хотите построить квартал?"
        )
    }

    private fun moveToBuildChoice(message: String = _gameState.value.message) {
        _gameState.update {
            it.copy(
                phase = GamePhase.BUILD_CHOICE,
                players = _players.toList(),
                message = message
            )
        }
    }

    fun startSelectCardPhase() {
        _gameState.update { it.copy(phase = GamePhase.SELECT_CARD) }
    }

    fun confirmSelectedCard(selectedCardName: String) {
        val activePlayer = _gameState.value.activePlayer
        val avlCards = quarterPool.getAvailableCards()

        val foundCard = avlCards.find { it.name == selectedCardName }

        if (foundCard == null) {
            updateMessage("Карта \"$selectedCardName\" не найдена в колоде! Проверьте правильность названия.")
            return
        }

        activePlayer?.addToHand(foundCard)
        quarterPool.drawCard(foundCard)
        logMove(MoveType.CARD_DRAWN, "${activePlayer?.name} взял карту \"${foundCard.name}\"")

        updateMessage("Игрок ${activePlayer?.name} получил карту \"${foundCard.name}\".")

        _gameState.update { currentState ->
            currentState.copy(
                phase = GamePhase.BUILD_CHOICE
            )
        }
    }

    fun buildDistrict(district: IDistrict?) {
        val player = _gameState.value.activePlayer ?: return

        if (district != null && player.gold >= district.cost) {
            player.build(district)
            logMove(MoveType.DISTRICT_BUILT, "${player.name} построил ${district.name} (цена ${district.cost})")
        }
        if (player.city.size >= 7) {
            isGameOverFlag = true
        }

        _gameState.update {
            it.copy(
                currentCharacterIndex = it.currentCharacterIndex + 1,
                players = _players.toList()
            )
        }
        round()
    }

    //Final of the game

    private fun finishGame() {
        _players.sortByDescending { it.city.sumOf { district -> district.cost } }

        val results = _players.joinToString("\n") { player ->
            "${player.name}: ${player.city.sumOf { it.cost }} очков"
        }

        val winner = _players.firstOrNull()
        val winnerScore = winner?.city?.sumOf { it.cost } ?: 0

        if (winner != null) {
            addResults(
                players = _players,
                winner = winner,
                score = winnerScore
            )
        }

        logMove(
            MoveType.GAME_OVER,
            "Игра окончена. Победитель: ${winner?.name} (${winnerScore} очков)",
            playerName = winner?.name
        )

        _gameState.update {
            it.copy(
                phase = GamePhase.GAME_OVER,
                message = "ИГРА ОКОНЧЕНА. РЕЗУЛЬТАТЫ:\n$results"
            )
        }
    }

    private fun updateMessage(msg: String) {
        _gameState.update { it.copy(message = msg) }
    }

    //abilities

    fun triggerActiveCharacterAbility() {
        val activePlayer = _gameState.value.activePlayer
        val character = domain.GameCharacters.getByOrder(_gameState.value.currentCharacterIndex)

        if (character == null) {
            skipAbility()
            return
        }
        if (character.rank in 4..7) {
            character.ability(activePlayer as Player?, _players, quarterPool)
            _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
        } else {
            _gameState.update { it.copy(phase = GamePhase.SPECIAL_ABILITY) }
        }
    }

    fun applyAssassinAbility(targetRank: Int) {
        val victim = getByOrder(targetRank)
        victim?.isKilled = true
        val msg = if (victim != null) "Ассасин убил персонажа: ${victim.name}" else "Никого не убили."
        updateMessage(msg)
        logMove(
            MoveType.PLAYER_KILLED,
            msg,
            characterName = "Ассасин",
            playerName = _gameState.value.activePlayer?.name
        )
        _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
    }

    fun applyThiefAbility(targetRank: Int) {
        val victimChar = getByOrder(targetRank)
        if (victimChar == null || victimChar.isKilled) {
            updateMessage("Нельзя ограбить этого персонажа (не в игре или убит).")
            _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
            return
        }

        if (targetRank == 2) {
            updateMessage("Вор не может ограбить сам себя.")
            _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
            return
        }

        thiefTargetRank = targetRank
        thiefName = _gameState.value.activePlayer?.name
        updateMessage("Вор выбрал целью персонажа ранга $targetRank.")
        logMove(
            MoveType.ABILITY_USED,
            "Вор выбрал целью персонажа ранга $targetRank",
            characterName = "Вор",
            playerName = _gameState.value.activePlayer?.name
        )
        _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
    }

    fun applySorcererSwap(targetName: String) {
        val activePlayer = _gameState.value.activePlayer ?: return
        val targetPlayer = findPlayerByName(targetName) ?: return

        if (targetPlayer == activePlayer) {
            updateMessage("Нельзя обменяться картами самим с собой!")
            return
        }

        performHandSwap(activePlayer, targetPlayer)
        moveToBuildChoice()
    }

    private fun performHandSwap(playerA: IPlayer, playerB: IPlayer) {
        val handA = playerA.hand.toList()
        val handB = playerB.hand.toList()

        playerA.replaceHand(handB)
        playerB.replaceHand(handA)

        updateMessage("Чародей ${playerA.name} успешно обменялся картами с игроком ${playerB.name}!")
        logMove(
            MoveType.HAND_SWAPPED,
            "Чародей ${playerA.name} обменялся картами с ${playerB.name}",
            characterName = "Чародей",
            playerName = playerA.name
        )
    }

    fun applyWarlordDestroy(targetPlayerName: String, districtName: String) {
        val activePlayer = _gameState.value.activePlayer ?: return
        val targetPlayer = findPlayerByName(targetPlayerName) ?: return
        val district = findDistrictInCity(targetPlayer, districtName) ?: return

        val costToDestroy = district.cost - 1
        if (!canAffordDestruction(activePlayer, costToDestroy)) return

        performDestruction(activePlayer, targetPlayer, district, costToDestroy)
    }

    private fun findPlayerByName(name: String): IPlayer? {
        val player = _players.find { it.name.equals(name, ignoreCase = true) }
        if (player == null) updateMessage("Игрок не найден.")
        return player
    }

    private fun findDistrictInCity(player: IPlayer, districtName: String): IDistrict? {
        val district = player.city.find { it.name.equals(districtName, ignoreCase = true) }
        if (district == null) updateMessage("У указанного игрока нет такого здания в городе.")
        return district
    }

    private fun canAffordDestruction(player: IPlayer, cost: Int): Boolean {
        if (player.gold < cost) {
            updateMessage("Недостаточно золота для разрушения (требуется $cost).")
            return false
        }
        return true
    }

    private fun performDestruction(
        activePlayer: IPlayer,
        targetPlayer: IPlayer,
        district: IDistrict,
        cost: Int
    ) {
        activePlayer.gold -= cost
        targetPlayer.destroyBuilding(district)
        quarterPool.discardCard(district)

        updateMessage("Кондотьер разрушил здание ${district.name} у игрока ${targetPlayer.name}!")
        logMove(
            MoveType.DISTRICT_DESTROYED,
            "Кондотьер разрушил ${district.name} у ${targetPlayer.name}",
            characterName = "Кондотьер",
            playerName = activePlayer.name
        )
        moveToBuildChoice()
    }

    fun openLeaderboard() {
        _gameState.update { it.copy(phase = GamePhase.LEADERBOARD) }
    }

    fun backToSetup() {
        _gameState.update { it.copy(phase = GamePhase.SETUP_PLAYERS) }
    }

    private val _moveHistory = mutableListOf<GameLogEntry>()
    val moveHistory: List<GameLogEntry> get() = _moveHistory.toList()
    private var currentRound: Int = 1
    private var moveHistoryVersion = MutableStateFlow(0) // для триггера рекомпозиции

    val historyVersion: StateFlow<Int> = moveHistoryVersion

    private fun logMove(
        type: MoveType,
        description: String,
        characterName: String? = _gameState.value.activeCharacterName.ifBlank { null },
        playerName: String? = _gameState.value.activePlayer?.name
    ) {
        _moveHistory.add(
            GameLogEntry(
                round = currentRound,
                characterName = characterName,
                playerName = playerName,
                type = type,
                description = description
            )
        )
        moveHistoryVersion.value = _moveHistory.size
    }

    fun resetHistory() {
        _moveHistory.clear()
        currentRound = 1
        moveHistoryVersion.value = 0
    }
}
