package unit

import database.DatabaseManager
import database.PlayersTable
import database.GameHistoryTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import engine.GamePhase
import engine.GameSession

class DatabaseManagerTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDb() {
            Database.connect("jdbc:sqlite::memory:", driver = "org.sqlite.JDBC")
        }
    }

    @BeforeEach
    fun cleanDb() {
        transaction {
            SchemaUtils.drop(PlayersTable, GameHistoryTable)
            SchemaUtils.create(PlayersTable, GameHistoryTable)
        }
    }

    @Test
    fun `registerOrGetPlayer inserts new player with zero stats`() {
        DatabaseManager.registerOrGetPlayer("Alice")

        val stats = DatabaseManager.getLeaderboard()
        assertEquals(1, stats.size)
        assertEquals("Alice", stats[0].name)
        assertEquals(0, stats[0].gamesPlayed)
        assertEquals(0, stats[0].wins)
    }

    @Test
    fun `registerOrGetPlayer is idempotent for the same name`() {
        DatabaseManager.registerOrGetPlayer("Alice")
        DatabaseManager.registerOrGetPlayer("Alice")
        DatabaseManager.registerOrGetPlayer("Alice")

        val stats = DatabaseManager.getLeaderboard()
        assertEquals(1, stats.size, "Повторная регистрация не должна создавать дубликаты")
    }

    @Test
    fun `recordGameResult increments gamesPlayed for everyone and wins only for winner`() {
        DatabaseManager.registerOrGetPlayer("Alice")
        DatabaseManager.registerOrGetPlayer("Bob")
        DatabaseManager.registerOrGetPlayer("Carol")

        DatabaseManager.recordGameResult(
            winnerName = "Bob",
            winnerScore = 25,
            allPlayerNames = listOf("Alice", "Bob", "Carol")
        )

        val stats = DatabaseManager.getLeaderboard().associateBy { it.name }

        assertEquals(1, stats["Alice"]!!.gamesPlayed)
        assertEquals(0, stats["Alice"]!!.wins)

        assertEquals(1, stats["Bob"]!!.gamesPlayed)
        assertEquals(1, stats["Bob"]!!.wins)

        assertEquals(1, stats["Carol"]!!.gamesPlayed)
        assertEquals(0, stats["Carol"]!!.wins)
    }
}

class LeaderboardPhaseTest {

    @Test
    fun `openLeaderboard switches phase to LEADERBOARD`() {
        val session = GameSession()
        assertEquals(GamePhase.SETUP_PLAYERS, session.gameState.value.phase)

        session.openLeaderboard()

        assertEquals(GamePhase.LEADERBOARD, session.gameState.value.phase)
    }

    @Test
    fun `backToSetup returns phase to SETUP_PLAYERS`() {
        val session = GameSession()
        session.openLeaderboard()
        session.backToSetup()

        assertEquals(GamePhase.SETUP_PLAYERS, session.gameState.value.phase)
    }

    @Test
    fun `openLeaderboard preserves registered players in state`() {
        val session = GameSession()
        session.addPlayer("Alice")
        session.addPlayer("Bob")
        session.addPlayer("Carol")
        session.addPlayer("Dave")

        session.openLeaderboard()
        session.backToSetup()

        assertEquals(4, session.gameState.value.players.size)
        assertEquals(
            listOf("Alice", "Bob", "Carol", "Dave"),
            session.gameState.value.players.map { it.name }
        )
    }
}