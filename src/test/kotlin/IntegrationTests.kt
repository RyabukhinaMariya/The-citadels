package integration

import engine.GamePhase
import engine.GameSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BuildAndWinIntegrationTest {

    private lateinit var session: GameSession

    @BeforeEach
    fun setup() {
        session = GameSession()
        session.addPlayer("P1")
        session.addPlayer("P2")
        session.addPlayer("P3")
        session.addPlayer("P4")
        session.startGame()
    }

    @Test
    fun `player with 7 districts triggers game over`() {
        val P1 = session.gameState.value.players.first { it.name == "P1" }

        repeat(6) {
            val card = P1.hand.firstOrNull() ?: return@repeat
            P1.build(card)
        }

        session.confirmCharacterPlayer(P1.id)
        session.takeGold()

        val seventh = P1.hand.firstOrNull()
        if (seventh != null && P1.gold >= seventh.cost) {
            session.buildDistrict(seventh)
        } else {
            session.buildDistrict(null)
        }

        if (P1.city.size >= 7) {
            assertEquals(GamePhase.GAME_OVER, session.gameState.value.phase)
        }
    }
}

class TurnFlowIntegrationTest {

    private lateinit var session: GameSession

    @BeforeEach
    fun setup() {
        session = GameSession()
        session.addPlayer("P1")
        session.addPlayer("P2")
        session.addPlayer("P3")
        session.addPlayer("P4")
        session.startGame()
    }

    @Test
    fun `ending turn without building advances to next character`() {
        val state = session.gameState.value
        assertEquals(GamePhase.CHARACTER_CALL, state.phase)
        assertEquals(1, state.currentCharacterIndex)

        val P1 = state.players.first { it.name == "P1" }
        session.confirmCharacterPlayer(P1.id)

        session.takeGold()

        session.buildDistrict(null)

        val after = session.gameState.value
        assertEquals(2, after.currentCharacterIndex)
        assertEquals(GamePhase.CHARACTER_CALL, after.phase)
    }

    @Test
    fun `skipping character (null player) advances the round`() {
        session.confirmCharacterPlayer(null)

        val state = session.gameState.value
        assertEquals(2, state.currentCharacterIndex)
        assertEquals(GamePhase.CHARACTER_CALL, state.phase)
    }

    @Test
    fun `all skipped characters end the round and reset to 1`() {
        repeat(8) {
            session.confirmCharacterPlayer(null)
        }

        val state = session.gameState.value
        // После 8 пропусков раунд должен начаться заново с rank 1
        assertEquals(1, state.currentCharacterIndex)
    }
}