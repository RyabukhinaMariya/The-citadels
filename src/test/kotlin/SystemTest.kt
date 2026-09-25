package integration

import engine.GameSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AbilityIntegrationTest {

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
    fun `Assassin kills target and target is skipped in same round`() {
        val state = session.gameState.value
        val P1 = state.players.first { it.name == "P1" }
        session.confirmCharacterPlayer(P1.id)
        session.takeGold()

        session.triggerActiveCharacterAbility()
        session.applyAssassinAbility(6)

        session.confirmCharacterPlayer(null) // 2
        session.confirmCharacterPlayer(null) // 3
        session.confirmCharacterPlayer(null) // 4
        session.confirmCharacterPlayer(null) // 5

        val after = session.gameState.value
        assertEquals(7, after.currentCharacterIndex)
    }

    @Test
    fun `Warlord destroys district from another player city`() {
        val state = session.gameState.value
        val P1 = state.players.first { it.name == "P1" }
        val P2 = state.players.first { it.name == "P2" }

        val P2District = P2.hand.first()
        P2.build(P2District)
        P1.gold = 10

        repeat(7) { session.confirmCharacterPlayer(null) }

        session.confirmCharacterPlayer(P1.id)
        session.takeGold()

        val P2CityBefore = P2.city.size
        session.triggerActiveCharacterAbility()
        session.applyWarlordDestroy(P2.name, P2District.name)

        assertEquals(P2CityBefore - 1, P2.city.size)
        assertFalse(P2.city.any { it.name == P2District.name })
    }
}