# The-citadels

```mermaid

classDiagram
    class GameSession {
        -quarterPool: QuarterPool
        -_players: MutableList~IPlayer~
        -isGameOverFlag: Boolean
        -_gameState: MutableStateFlow~GameState~
        +gameState: StateFlow~GameState~
        +addPlayer(name)
        +startGame()
        +confirmCharacterPlayer(playerId)
        +takeGold()
        +startSelectCardPhase()
        +confirmSelectedCard(name)
        +buildDistrict(district)
        +triggerActiveCharacterAbility()
        +skipAbility()
        +applyAssassinAbility(rank)
        +applyThiefAbility(rank)
        +applySorcererSwap(name)
        +applyWarlordDestroy(player, district)
    }

    class GameState {
        +phase: GamePhase
        +players: List~IPlayer~
        +currentCharacterIndex: Int
        +activePlayer: IPlayer?
        +activeCharacterName: String
        +message: String
        +abilityDialog: AbilityDialogState
    }

    class GamePhase {
        <<enumeration>>
        SETUP_PLAYERS
        ROUND_START
        CHARACTER_CALL
        SPECIAL_ABILITY
        ACTION_CHOICE
        BUILD_CHOICE
        SELECT_CARD
        GAME_OVER
    }

    class SetupScreen {
        +session: GameSession
        +players: List~IPlayer~
    }
    class CharacterCallScreen {
        +session: GameSession
        +players: List~IPlayer~
        +characterName: String
    }
    class ActionChoiceScreen {
        +session: GameSession
        +activePlayer: IPlayer?
    }
    class BuildAndAbilityScreen {
        +session: GameSession
        +activePlayer: IPlayer?
    }
    class SelectCardScreen {
        +onCardSelected: (String) -> Unit
    }
    class AbilityInputScreen {
        +session: GameSession
        +characterName: String?
        +characterRank: Int
    }
    class GameOverScreen {
        +results: String
    }
    class DistrictCardView {
        +district: IDistrict
        +onClick: () -> Unit
    }

    GameSession --> GameState
    GameState --> GamePhase
    GameSession ..> SetupScreen : gives a state
    GameSession ..> CharacterCallScreen : gives a state
    GameSession ..> ActionChoiceScreen : gives a state
    GameSession ..> BuildAndAbilityScreen :gives a state
    GameSession ..> SelectCardScreen 
    GameSession ..> AbilityInputScreen
    GameSession ..> GameOverScreen : gives a message
    BuildAndAbilityScreen --> DistrictCardView
