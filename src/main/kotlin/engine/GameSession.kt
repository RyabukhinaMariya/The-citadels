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

//phases of the game
enum class GamePhase {
    SETUP_PLAYERS,
    ROUND_START,
    CHARACTER_CALL,
    SPECIAL_ABILITY,
    ACTION_CHOICE,
    BUILD_CHOICE,
    SELECT_CARD,
    GAME_OVER
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

        // переделать на еще экран
        val available = quarterPool.getAvailableCards()
        for (i in 0..3) {
            if (available.size > i) {
                val card = available[i]
                player.addToHand(card)
                quarterPool.drawCard(card)
            }
        }

        _gameState.update { it.copy(players = _players.toList(), message = "Игрок $name добавлен.") }
    }

    fun startGame() {
        if (_players.size < 4) {
            updateMessage("Нужно минимум 4 игрока для старта.")
            return
        }

        resetCharactersState()
        _gameState.update { it.copy(phase = GamePhase.ROUND_START, currentCharacterIndex = 1) }
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

        var index = _gameState.value.currentCharacterIndex
        var character = getByOrder(index)

        //searching for next alive character
        while (index <= 8) {
            character = getByOrder(index)
            if (character != null && !character.isKilled) break
            index++
        }

        if (index > 8 || character == null) {
            resetCharactersState()
            _gameState.update { it.copy(currentCharacterIndex = 1, phase = GamePhase.ROUND_START) }
            round()
            return
        }

        _gameState.update {
            it.copy(
                phase = GamePhase.CHARACTER_CALL,
                currentCharacterIndex = index,
                activeCharacterName = character.name,
                activePlayer = null,
                message = "Персонаж ${character.name}. Кто за него играет? (Выберите игрока или пропуск)"
            )
        }
    }

    fun confirmCharacterPlayer(playerId: Int?) {
        if (_gameState.value.phase != GamePhase.CHARACTER_CALL) return

        if (playerId == null) {
            _gameState.update { it.copy(currentCharacterIndex = it.currentCharacterIndex + 1) }
            round()
            return
        }

        val player = _players.find { it.id == playerId } ?: run {
            updateMessage("Игрок не найден.")
            return
        }

        val currentRank = _gameState.value.currentCharacterIndex

        if (currentRank == thiefTargetRank) {
            val thief = _players.find { p ->
                p.name == thiefName
            }
            val stolen = player.gold
            player.gold = 0
            thief?.let { it.gold += stolen }
            updateMessage("Вор ограбил ${player.name} на $stolen золотых!")
            thiefTargetRank = null
        }

        _gameState.update {
            it.copy(
                phase = GamePhase.ACTION_CHOICE,
                activePlayer = player,
                message = "${player?.name}, возьмите 2 золотых или вытяните карту."
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

        _gameState.update {
            it.copy(
                phase = GamePhase.BUILD_CHOICE,
                players = _players.toList(),
                message = "${player.name} получил 2 золотых. Хотите построить квартал?"
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
        updateMessage(if (victim != null) "Ассасин убил персонажа: ${victim.name}" else "Никого не убили.")
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
        updateMessage("Вор выбрал целью персонажа ранга $targetRank.")
        _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
    }

    fun applySorcererSwap(targetName: String) {
        val activePlayer = _gameState.value.activePlayer ?: return
        val targetPlayer = _players.find { it.name.equals(targetName.trim(), ignoreCase = true) }

        if (targetPlayer == null) {
            updateMessage("Игрок \"$targetName\" не найден!")
            return
        }

        if (targetPlayer == activePlayer) {
            updateMessage("Нельзя обменяться картами самим с собой!")
            return
        }

        val myHand = activePlayer.hand.toList()
        val targetHand = targetPlayer.hand.toList()

        activePlayer.replaceHand(targetHand)
        targetPlayer.replaceHand(myHand)

        updateMessage("Чародей ${activePlayer.name} успешно обменялся картами с игроком ${targetPlayer.name}!")

        _gameState.update { currentState ->
            currentState.copy(
                phase = GamePhase.BUILD_CHOICE
            )
        }
    }

    fun applyWarlordDestroy(targetPlayerName: String, districtName: String) {
        val activePlayer = _gameState.value.activePlayer ?: return
        val targetPlayer = _players.find { it.name.equals(targetPlayerName, ignoreCase = true) }

        if (targetPlayer == null) {
            updateMessage("Игрок не найден.")
            return
        }

        val district = targetPlayer.city.find { it.name.equals(districtName, ignoreCase = true) }
        if (district == null) {
            updateMessage("У указанного игрока нет такого здания в городе.")
            return
        }

        val costToDestroy = district.cost - 1
        if (activePlayer.gold < costToDestroy) {
            updateMessage("Недостаточно золота для разрушения (требуется ${costToDestroy}).")
            return
        }

        activePlayer.gold -= costToDestroy
        targetPlayer.destroyBuilding(district)
        quarterPool.discardCard(district)

        updateMessage("Кондотьер разрушил здание ${district.name} у игрока ${targetPlayer.name}!")
        _gameState.update { it.copy(phase = GamePhase.BUILD_CHOICE) }
    }
}