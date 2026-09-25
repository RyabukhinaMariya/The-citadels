classDiagram
    direction LR
``mermaid

    class ICard {
        <<interface>>
        +String name
    }

    class IDistrict {
        <<interface>>
        +Int cost
        +QuarterColor color
    }

    class IPlayer {
        <<interface>>
        +Int id
        +Int gold
        +List~IDistrict~ hand
        +List~IDistrict~ city
        +canAfford(district) Boolean
        +discardFromHand(district)
        +destroyBuilding(district)
        +addToHand(district)
        +replaceHand(list)
        +build()
    }

    class ICharacter {
        <<interface>>
        +Int rank
        +Boolean isKilled
        +ability(currentPlayer, allPlayers, pool)
    }

    ICard <|-- IDistrict
    ICard <|-- IPlayer
    ICard <|-- ICharacter

    class Player {
        +String name
        +Int id
        +Int gold
        +MutableList~IDistrict~ hand
        +MutableList~IDistrict~ city
        +build(district)
    }
    IPlayer <|.. Player

    class Watchtower
    class Prison
    class Battlefield
    class Fortress
    class Tavern
    class Market
    class Shop
    class Docks
    class Harbor
    class TownHall
    class Temple
    class Church
    class Monastery
    class Cathedral
    class Manor
    class Palace
    class Castle

    IDistrict <|.. Watchtower
    IDistrict <|.. Prison
    IDistrict <|.. Battlefield
    IDistrict <|.. Fortress
    IDistrict <|.. Tavern
    IDistrict <|.. Market
    IDistrict <|.. Shop
    IDistrict <|.. Docks
    IDistrict <|.. Harbor
    IDistrict <|.. TownHall
    IDistrict <|.. Temple
    IDistrict <|.. Church
    IDistrict <|.. Monastery
    IDistrict <|.. Cathedral
    IDistrict <|.. Manor
    IDistrict <|.. Palace
    IDistrict <|.. Castle

    class Assassin
    class Thief
    class Sorcerer
    class King
    class Bishop
    class Merchant
    class Architect
    class Warlord

    ICharacter <|.. Assassin
    ICharacter <|.. Thief
    ICharacter <|.. Sorcerer
    ICharacter <|.. King
    ICharacter <|.. Bishop
    ICharacter <|.. Merchant
    ICharacter <|.. Architect
    ICharacter <|.. Warlord
