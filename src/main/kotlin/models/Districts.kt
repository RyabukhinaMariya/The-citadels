package models

import domain.IDistrict
import domain.IPlayer
import domain.Player
import domain.QuarterColor
import engine.GameSession

//RED
class Watchtower : IDistrict {
    override val name: String = "Смотровая башня"
    override val cost: Int = 1
    override val color: QuarterColor = QuarterColor.RED
}

class Prison : IDistrict {
    override val name: String = "Темница"
    override val cost: Int = 2
    override val color: QuarterColor = QuarterColor.RED
}

class Battlefield: IDistrict {
    override val name: String = "Поле боя"
    override val cost: Int = 3
    override val color: QuarterColor = QuarterColor.RED
}

class Fortress: IDistrict {
    override val name: String = "Крепость"
    override val cost: Int = 5
    override val color: QuarterColor = QuarterColor.RED
}

//GREEN
class Tavern : IDistrict {
    override val name: String = "Таверна"
    override val cost: Int = 1
    override val color: QuarterColor = QuarterColor.GREEN
}

class Market : IDistrict {
    override val name: String = "Рынок"
    override val cost: Int = 2
    override val color: QuarterColor = QuarterColor.GREEN
}

class Shop : IDistrict {
    override val name: String = "Магазин"
    override val cost: Int = 2
    override val color: QuarterColor = QuarterColor.GREEN
}

class Docks : IDistrict {
    override val name: String = "Доки"
    override val cost: Int = 3
    override val color: QuarterColor = QuarterColor.GREEN
}

class Harbor : IDistrict {
    override val name: String = "Гавань"
    override val cost: Int = 4
    override val color: QuarterColor = QuarterColor.GREEN
}

class TownHall : IDistrict {
    override val name: String = "Ратуша"
    override val cost: Int = 5
    override val color: QuarterColor = QuarterColor.GREEN
}

//BLUE
class Temple : IDistrict {
    override val name: String = "Храм"
    override val cost: Int = 1
    override val color: QuarterColor = QuarterColor.BLUE
}

class Church : IDistrict {
    override val name: String = "Церковь"
    override val cost: Int = 2
    override val color: QuarterColor = QuarterColor.BLUE
}

class Monastery : IDistrict {
    override val name: String = "Монастырь"
    override val cost: Int = 3
    override val color: QuarterColor = QuarterColor.BLUE
}

class Cathedral : IDistrict {
    override val name: String = "Собор"
    override val cost: Int = 5
    override val color: QuarterColor = QuarterColor.BLUE
}

//YELLOW
class Manor : IDistrict {
    override val name: String = "Поместье"
    override val cost: Int = 3
    override val color: QuarterColor = QuarterColor.YELLOW
}

class Palace : IDistrict {
    override val name: String = "Дворец"
    override val cost: Int = 5
    override val color: QuarterColor = QuarterColor.YELLOW
}

class Castle : IDistrict {
    override val name: String = "Замок"
    override val cost: Int = 4
    override val color: QuarterColor = QuarterColor.YELLOW
}
