package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import domain.GameCharacters.getByOrder
import domain.ICharacter
import domain.Player
import engine.QuarterPool
import models.CitadelConfig

enum class AppScreen {
    START_MENU,
    CARD_SETUP,
    GAME_BOARD,
    IN_GAME,
    ASSASSIN_ABILITY,
    THIEF_ABILITY,
    SORCERER_ABILITY,
    GET_CARD,
    WHO_PLAYS_FOR,
    BUILD_DISTRICT
}

@Composable

fun MainNavigation() {

    var currentScreen by remember { mutableStateOf(AppScreen.START_MENU) }
    var players by remember { mutableStateOf(listOf<Player>()) }
    var currentPlayer by remember { mutableStateOf<Player?>(null) }
    var currentCharacter by remember { mutableStateOf<ICharacter?>(null) }
    var cardCount by remember { mutableStateOf(0) }
    var makeDes by remember { mutableStateOf(false) }

    val baseDeck = remember { CitadelConfig.getBaseDeck() }
    val pool = QuarterPool (baseDeck)


    when (currentScreen) {
        AppScreen.START_MENU -> {
            GameStartScreen(onGameStart = { registeredPlayers ->
                players = registeredPlayers
                currentScreen = AppScreen.IN_GAME

            })
        }


        AppScreen.CARD_SETUP -> {
            CardSetupScreen(
                playersList = players,
                initialDistricts = baseDeck,
                onCardsDistributed = { updatedPlayers ->
                    players = updatedPlayers
                    currentScreen = AppScreen.IN_GAME

                }
            )
        }

        AppScreen.GAME_BOARD -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GetCardsOrMoney(player = currentPlayer, onAnswerConfirmed = {answer ->
                    if (!makeDes) {
                        if (answer) {
                            currentPlayer?.gold += 2
                        }
                        else {
                            print(currentCharacter?.rank)
                            if (currentCharacter?.rank != 7) {
                                cardCount = 1
                                currentScreen = AppScreen.GET_CARD
                            } else {
                                cardCount = 3
                                currentScreen = AppScreen.GET_CARD
                            }
                        }
                        makeDes = true
                    }
                    else {
                        currentScreen = AppScreen.IN_GAME
                    }
                })
            }
        }

        AppScreen.IN_GAME -> {
            var currentCharacterOrder by remember { mutableStateOf(1) }
            val character = getByOrder(currentCharacterOrder)
            makeDes = false

            if (character != null) {
                IsInGame(character = character, onAnswerConfirmed = { isAccepted ->
                    if (isAccepted) {
                        currentCharacter = getByOrder(currentCharacterOrder)
                        if (currentCharacterOrder == 1) {
                            currentScreen = AppScreen.ASSASSIN_ABILITY
                        }
                        else if (currentCharacterOrder == 2) {
                            currentScreen = AppScreen.THIEF_ABILITY
                        }
                        else if (currentCharacterOrder == 3) {
                            currentScreen = AppScreen.SORCERER_ABILITY
                        }
                        else if (currentCharacterOrder in 4..6) {
                            currentScreen = AppScreen.WHO_PLAYS_FOR
                            character.ability(currentPlayer, players, pool)
                        }
                        else {
                            currentScreen = AppScreen.GAME_BOARD
                        }
                    }
                    currentCharacterOrder++

                    if (currentCharacterOrder > 8) {
                        currentCharacterOrder = 1
                    }
                })
            }
        }

        AppScreen.ASSASSIN_ABILITY -> {
            AssassinSpecRule(onTargetSelected = {
                currentScreen = AppScreen.GAME_BOARD })

        }


        AppScreen.THIEF_ABILITY -> {
            ThiefSpecRule(playersList = players,  onAnswerConfirmed = {isApplied, thiefName, victimName ->
                if (isApplied) {
                    val thief = players.find {it.name.equals(thiefName, ignoreCase = true)}
                    val victim = players.find { it.name.equals(victimName, ignoreCase = true) }
                    if (victim != null && thief != null) {
                        thief.gold += victim.gold
                        victim.gold = 0
                    }
                }
                currentScreen = AppScreen.GAME_BOARD

            })
        }

        AppScreen.SORCERER_ABILITY -> {
            SorcererSpecRule(playersList = players, onAnswerConfirmed = {isApplied, sorcererName, victimName ->
                val sorcerer = players.find {it.name.equals(sorcererName, ignoreCase = true)}
                print(isApplied)
                if (isApplied) {
                    val victim = players.find { it.name.equals(victimName, ignoreCase = true) }
                    if (victim != null && sorcerer != null) {
                        val victimHand = victim.hand
                        victim.replaceHand(sorcerer.hand)
                        sorcerer.replaceHand(victimHand)
                        cardCount = currentPlayer?.hand?.size as Int
                        currentScreen = AppScreen.GAME_BOARD
                    }
                }
                else {
                    currentPlayer = sorcerer
                    currentScreen = AppScreen.GET_CARD

                }

            })

        }

        AppScreen.GET_CARD -> {
            print(cardCount)
            GetCards(initialDistricts = baseDeck, player = currentPlayer, amount = cardCount, onCardsDistributed = {})
            currentScreen = AppScreen.GAME_BOARD
        }

        AppScreen.WHO_PLAYS_FOR -> {
            WhoPlaysFor(playersList = players, character = currentCharacter, onAnswerConfirmed = {player ->
                currentPlayer = player
                currentScreen = AppScreen.GAME_BOARD
            })
        }

        AppScreen.BUILD_DISTRICT -> {
            BuildQuarter(currentPlayer, onAnswerConfirmed = {isAccepted ->
                currentScreen = AppScreen.GAME_BOARD
            })
        }
    }
}