package domain

interface ICharacter: ICard {
    val rank: Int
    var isKilled: Boolean
    fun ability(
        currentPlayer: IPlayer,
        allPlayers: List<IPlayer>,
        pool: QuarterPool
    )
}