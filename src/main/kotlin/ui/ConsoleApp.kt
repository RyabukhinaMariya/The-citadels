package ui

import engine.GameSession

fun main() {
    val app = ConsoleApp()
    app.start()
}

class ConsoleApp {
    private lateinit var game: GameSession

    fun start() {
        println("=== Добро пожаловать! ===")

    }
}