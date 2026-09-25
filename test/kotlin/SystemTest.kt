package system

import engine.GameSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class SystemGameFlowTest {

    @Test
    fun `full game flow reaches game over after a city reaches 7 districts`() {
        val sb = StringBuilder()
        sb.appendLine("4") // 4 игрока
        sb.appendLine("P1")
        repeat(4) {sb.appendLine("Таверна")}
        sb.appendLine("P2")
        repeat(4) {sb.appendLine("Таверна")}
        sb.appendLine("P3")
        repeat(4) {sb.appendLine("Таверна")}
        sb.appendLine("P4")
        repeat(4) {sb.appendLine("Таверна")}

        repeat(50) {
            sb.appendLine("P1")   // в игре
            sb.appendLine("M")    // берёт деньги
            sb.appendLine("Y")    // строит
            sb.appendLine("Таверна")
            repeat(7) { sb.appendLine("N") }
        }

        System.setIn(ByteArrayInputStream(sb.toString().toByteArray()))

        val session = GameSession()
        val t = Thread { session.setupGame() }
        t.isDaemon = true
        t.start()

        val playersField = GameSession::class.java.getDeclaredField("players")
        playersField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val players = playersField.get(session) as List<domain.Player>

        val p1 = players.first { it.name == "P1" }
        assertTrue(p1.city.size >= 7, "P1 не построил 7+ кварталов, построено ${p1.city.size}")
    }
}