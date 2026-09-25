package database

import domain.IPlayer
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseManager {

    fun init() {
        Database.connect("jdbc:sqlite:citadels.db", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(PlayersTable, GameHistoryTable)
        }
    }

    fun registerOrGetPlayer(playerName: String) {
        transaction {
            val existing = PlayersTable.select { PlayersTable.name eq playerName }.singleOrNull()
            if (existing == null) {
                PlayersTable.insert {
                    it[name] = playerName
                    it[gamesPlayed] = 0
                    it[wins] = 0
                }
            }
        }
    }

    fun recordGameResult(winnerName: String, winnerScore: Int, allPlayerNames: List<String>) {
        transaction {
            GameHistoryTable.insert {
                it[this.winnerName] = winnerName
                it[this.winnerScore] = winnerScore
                it[totalPlayers] = allPlayerNames.size
                it[playedAt] = LocalDateTime.now()
            }

            for (pName in allPlayerNames) {
                val isWinner = (pName == winnerName)
                PlayersTable.update({ PlayersTable.name eq pName }) {
                    with(SqlExpressionBuilder) {
                        it[gamesPlayed] = gamesPlayed + 1
                        if (isWinner) {
                            it[wins] = wins + 1
                        }
                    }
                }
            }
        }
    }

    fun getLeaderboard(): List<PlayerStats> {
        return transaction {
            PlayersTable.selectAll()
                .orderBy(PlayersTable.wins to SortOrder.DESC)
                .map {
                    PlayerStats(
                        name = it[PlayersTable.name],
                        gamesPlayed = it[PlayersTable.gamesPlayed],
                        wins = it[PlayersTable.wins]
                    )
                }
        }
    }

    fun addResults(players: MutableList<IPlayer>, winner: IPlayer, score: Int) {
        val allNames = players.map { it.name }
        DatabaseManager.recordGameResult(
            winnerName = winner.name,
            winnerScore = score,
            allPlayerNames = allNames
        )
    }
}

data class PlayerStats(
    val name: String,
    val gamesPlayed: Int,
    val wins: Int
)

// Players table
object PlayersTable : Table("players") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    val gamesPlayed = integer("games_played").default(0)
    val wins = integer("wins").default(0)

    override val primaryKey = PrimaryKey(id)
}

// Game History Table
object GameHistoryTable : Table("game_history") {
    val id = integer("id").autoIncrement()
    val winnerName = varchar("winner_name", 50)
    val totalPlayers = integer("total_players")
    val winnerScore = integer("winner_score")
    val playedAt = datetime("played_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}