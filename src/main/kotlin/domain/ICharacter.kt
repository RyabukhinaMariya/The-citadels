package domain
import engine.QuarterPool

interface ICharacter: ICard {
    val rank: Int
    var isKilled: Boolean
    fun ability(activePlayer: Player?, players: MutableList<IPlayer>, quarterPool: QuarterPool)
}
