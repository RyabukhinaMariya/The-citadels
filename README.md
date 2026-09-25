```mermaid
classDiagram
    direction TB

    class InputProvider {
        Interface
        readInput() String
    }

    class ConsoleInput {
        readInput() String
    }
    InputProvider <|.. ConsoleInput

    class QuarterPool {
        MutableMap~IDistrict,Int~ districtCards
        findDistrictCard(cardName) IDistrict
        drawCard() IDistrict
        discardCard(player) IDistrict
    }

    class GameSession {
        QuarterPool quarterPool
        MutableList~IPlayer~ players
        Boolean isGameOver
        getStartCards(player)
        setupGame()
        gameLoop(isGameOver)
        playRound()
        showFinalResults()
    }

    class CitadelConfig {
        Object
        getBaseDeck() Map~IDistrict,Int~
    }

    class GameCharacters {
        Object
        List~ICharacter~ all
        getByOrder(rank) ICharacter
    }

    class QuarterColor {
      Enumeration
        YELLOW
        BLUE
        GREEN
        RED
    }

    class Main {
        main()
    }

    Main ..> GameSession : create
    GameSession *-- QuarterPool
    GameSession o-- IPlayer
    QuarterPool ..> IDistrict : store
    CitadelConfig ..> IDistrict : create
    GameCharacters ..> ICharacter : store
    IDistrict ..> QuarterColor
    ConsoleInput ..> InputProvider
    GameSession ..> ConsoleInput
    ICharacter ..> QuarterPool : use
    ICharacter ..> IPlayer : 
