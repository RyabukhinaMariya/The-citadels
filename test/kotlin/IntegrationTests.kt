package engine

import domain.QuarterPool
import domain.CitadelConfig
import models.Tavern
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class QuarterPoolTest {

    @Test
    fun `drawCard reduces count in pool and returns correct card`() {
        val pool = QuarterPool(CitadelConfig.getBaseDeck())
        val tavern = pool.districtCards.keys.first { it.name == "Таверна" }
        val before = pool.districtCards[tavern]!!

        System.setIn(ByteArrayInputStream("Таверна\n".toByteArray()))
        val drawn = pool.drawCard()

        assertEquals("Таверна", drawn.name)
        assertEquals(before - 1, pool.districtCards[tavern])
    }

    @Test
    fun `discardCard returns card to pool and removes it from player hand`() {
        val pool = QuarterPool(CitadelConfig.getBaseDeck())
        val player = domain.Player("Alice", 1)
        val tavern = Tavern()
        player.addToHand(tavern)


        System.setIn(ByteArrayInputStream("Таверна\n".toByteArray()))
        val discarded = pool.discardCard(player)

        assertEquals("Таверна", discarded.name)
        assertEquals(1, pool.districtCards[tavern])
    }
}