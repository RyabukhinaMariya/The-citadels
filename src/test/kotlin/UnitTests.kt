package unit

import domain.Player
import models.Tavern
import models.Temple
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerTest {

    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        player = Player(name = "Alice", id = 1)
    }

    @Test
    fun `new player starts with 2 gold and empty hand and city`() {
        assertEquals(2, player.gold)
        assertTrue(player.hand.isEmpty())
        assertTrue(player.city.isEmpty())
    }

    @Test
    fun `build moves card from hand to city and deducts gold`() {
        val tavern = Tavern() // cost = 1
        player.addToHand(tavern)

        player.build(tavern)

        assertEquals(1, player.gold)              // 2 - 1
        assertFalse(player.hand.contains(tavern))
        assertTrue(player.city.contains(tavern))
    }

    @Test
    fun `destroyBuilding removes district from city`() {
        val temple = Temple() // cost = 1
        player.addToHand(temple)
        player.build(temple)

        player.destroyBuilding(temple)

        assertFalse(player.city.contains(temple))
    }

    @Test
    fun `canAfford returns true only when gold is enough`() {
        val temple = Temple() // cost 1
        assertTrue(player.canAfford(temple))

        player.gold = 0
        assertFalse(player.canAfford(temple))
    }

    @Test
    fun `replaceHand swaps hand contents completely`() {
        val a = Tavern()
        val b = Temple()
        player.addToHand(a)

        player.replaceHand(listOf(b))

        assertEquals(1, player.hand.size)
        assertTrue(player.hand.contains(b))
        assertFalse(player.hand.contains(a))
    }
}