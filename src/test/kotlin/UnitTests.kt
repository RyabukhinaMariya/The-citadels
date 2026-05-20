import domain.IDistrict
import domain.Player
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class UnitTests {

    @Test
    fun buildDistrict() {
        val player = Player(name = "Aglaya", id = 1)
        player.gold = 5

        val distr = mockk<IDistrict>()
        every { distr.cost } returns 3

        val result = player.canAfford(distr)

        assertTrue(result)
    }

    @Test
    fun equalDistrict() {
        val player = Player(name = "Aggot", id = 2)
        val market = mockk<IDistrict> { every { name } returns "Рынок" }

        player.city.add(market)

        val canBuildEqual = player.city.none { it.name == market.name }

        assertFalse { canBuildEqual }
    }

    @Test
    fun addDistrictToHand() {
        val player = Player(name = "Aglaya", id = 3)
        val market = mockk<IDistrict> { every {name} returns "Рынок"}

        player.addToHand(market)

        assertTrue { player.hand.contains(market) }
    }

    @Test
    fun discardDistrictFromHand() {
        val player = Player(name = "Aglaya", id = 4)
        val market = mockk<IDistrict>() { every {name} returns "Рынок"}

        player.addToHand(market)
        player.discardFromHand(market)

        assertFalse { player.hand.contains(market) }
    }

    @Test
    fun replaceHands() {
        val player = Player(name = "Aglaya", id = 5)

        val market = mockk<IDistrict>() { every {name} returns "Рынок"}
        val docks = mockk<IDistrict>() { every {name} returns "Доки"}

        player.addToHand(market)
        val newHand = listOf(docks)

        player.replaceHand(newHand)

        assertTrue { player.hand.contains(docks) }
        assertFalse { player.hand.contains(market) }
    }
}