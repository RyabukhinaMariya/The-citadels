package domain

import models.Tavern
import models.Temple
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlayerTest {

    @Test
    fun `player canAfford returns true when gold is enough`() {
        val player = Player("Alice", 1)
        val district = Tavern() // cost = 1
        player.gold = 5

        assertTrue(player.canAfford(district))
    }

    @Test
    fun `player canAfford returns false when gold is not enough`() {
        val player = Player("Bob", 1)
        val district = models.Palace() // cost = 5
        player.gold = 2

        assertFalse(player.canAfford(district))
    }

    @Test
    fun `build moves card from hand to city and deducts gold`() {
        val player = Player("Carol", 1)
        val district = Temple() // cost = 1
        player.gold = 3
        player.addToHand(district)

        player.build(district)

        assertEquals(2, player.gold)
        assertTrue(player.hand.isEmpty())
        assertEquals(1, player.city.size)
        assertEquals(district, player.city.first())
    }
}