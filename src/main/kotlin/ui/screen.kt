package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import domain.ICharacter
import domain.IDistrict
import domain.Player
import engine.QuarterPool

@Composable
fun GameStartScreen(onGameStart: (List<Player>) -> Unit) {
    var newPlayerName by remember { mutableStateOf("") }
    var registeredPlayers by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Добро пожаловать в менеджер игры Цитадели!",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "Регистрация игроков (4-7 человек)",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = newPlayerName,
            onValueChange = {
                newPlayerName = it
                errorMessage = ""
            },
            label = { Text("Имя игрока") },
            enabled = registeredPlayers.size < 7,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val trimmedName = newPlayerName.trim()

                when {
                    trimmedName.isBlank() -> {
                        errorMessage = "Имя не может быть пустым."
                    }
                    registeredPlayers.any { it.equals(trimmedName, ignoreCase = true) } -> {
                        errorMessage = "Это имя уже занято."
                    }
                    registeredPlayers.size >= 7 -> {
                        errorMessage = "Максимум 7 игроков."
                    }
                    else -> {
                        registeredPlayers = registeredPlayers + trimmedName
                        newPlayerName = ""
                        errorMessage = ""
                    }
                }
            },
            enabled = registeredPlayers.size < 7
        ) {
            Text("Добавить игрока")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Список игроков (${registeredPlayers.size}):", style = MaterialTheme.typography.h6)
        Column(modifier = Modifier.padding(8.dp)) {
            registeredPlayers.forEachIndexed { index, name ->
                Text("${index + 1}. $name", style = MaterialTheme.typography.body1)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (registeredPlayers.size in 4..7) {
                    val finalPlayersList = registeredPlayers.mapIndexed { index, name ->
                        Player(name = name, id = index + 1)
                    }
                    onGameStart(finalPlayersList)
                } else {
                    errorMessage = "Для старта игры нужно от 4 до 7 игроков!"
                }
            },
            enabled = registeredPlayers.size in 4..7,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
        ) {
            Text("Начать партию", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
fun CardSetupScreen(
    playersList: List<Player>,
    initialDistricts: Map<IDistrict, Int>,
    onCardsDistributed: (List<Player>) -> Unit
) {
    var currentPlayerIndex by remember { mutableStateOf(0) }
    val currentPlayer = playersList[currentPlayerIndex]

    var cardNameInput by remember { mutableStateOf("") }
    var currentCardsList by remember { mutableStateOf(listOf<IDistrict>()) }
    var errorMessage by remember { mutableStateOf("") }

    // ИСПОЛЬЗУЕМ mutableStateMapOf ДЛЯ РЕАКТИВНОСТИ
    val districtCards = remember {
        mutableStateMapOf<IDistrict, Int>().apply { putAll(initialDistricts) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "=== Ввод стартовых карт ===", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Игрок [${currentPlayerIndex + 1}/${playersList.size}]: ${currentPlayer.name}",
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Введено карт: ${currentCardsList.size} из 4")
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(currentCardsList) { card ->
                Text("• ${card.name}", style = MaterialTheme.typography.body1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = cardNameInput,
            onValueChange = {
                cardNameInput = it
                errorMessage = ""
            },
            label = { Text("Название карты квартала") },
            enabled = currentCardsList.size < 4,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val trimmedCard = cardNameInput.trim()
                val foundCard = districtCards.keys.find { it.name.equals(trimmedCard, ignoreCase = true) }

                when {
                    trimmedCard.isBlank() -> errorMessage = "Название карты не может быть пустым."
                    foundCard == null -> errorMessage = "Такой карты нет в колоде Цитаделей!"
                    (districtCards[foundCard] ?: 0) <= 0 -> errorMessage = "Такие карты закончились."
                    else -> {
                        currentCardsList = currentCardsList + foundCard
                        districtCards[foundCard] = (districtCards[foundCard] ?: 0) - 1
                        cardNameInput = ""
                        errorMessage = ""
                    }
                }
            },
            enabled = currentCardsList.size < 4
        ) {
            Text("Записать карту")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (currentCardsList.size == 4) {
                    for (card in currentCardsList) {
                        currentPlayer.addToHand(card)
                    }

                    if (currentPlayerIndex < playersList.size - 1) {
                        currentPlayerIndex++
                        currentCardsList = emptyList()
                        cardNameInput = ""
                    } else {
                        onCardsDistributed(playersList)
                    }
                } else {
                    errorMessage = "Необходимо ввести ровно 4 карты для игрока!"
                }
            },
            enabled = currentCardsList.size == 4,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (currentPlayerIndex == playersList.size - 1) Color(0xFF4CAF50) else MaterialTheme.colors.secondary
            )
        ) {
            val buttonText = if (currentPlayerIndex == playersList.size - 1) "Завершить и начать партию" else "Далее (Следующий игрок)"
            Text(buttonText, color = Color.White)
        }
    }
}

@Composable
fun IsInGame(character: ICharacter, onAnswerConfirmed: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Персонаж ${character.name} в раунде? ===",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "ДА"
            Button(
                onClick = { onAnswerConfirmed(true) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Да", color = Color.White, style = MaterialTheme.typography.button)
            }

            // Кнопка "НЕТ"
            Button(
                onClick = { onAnswerConfirmed(false) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Нет", color = Color.White, style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
fun AssassinSpecRule(
    onTargetSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Кого убивает ассасин? ===",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Первый ряд персонажей (Вор, Чародей, Король)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onTargetSelected("Вор") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Вор", color = Color.White, style = MaterialTheme.typography.button)
            }

            Button(
                onClick = { onTargetSelected("Чародей") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Чародей", color = Color.White, style = MaterialTheme.typography.button)
            }

            Button(
                onClick = { onTargetSelected("Король") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Король", color = Color.White, style = MaterialTheme.typography.button)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Второй ряд персонажей (Купец, Епископ, Архитектор, Кондотьер)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onTargetSelected("Купец") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Купец", color = Color.White, style = MaterialTheme.typography.button)
            }

            Button(
                onClick = { onTargetSelected("Епископ") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Епископ", color = Color.White, style = MaterialTheme.typography.button)
            }

            Button(
                onClick = { onTargetSelected("Архитектор") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Архитектор", color = Color.White, style = MaterialTheme.typography.button)
            }

            Button(
                onClick = { onTargetSelected("Кондотьер") },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Кондотьер", color = Color.White, style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
fun ThiefSpecRule(
    playersList: List<Player>,
    onAnswerConfirmed: (isApplied: Boolean, thiefName: String?, victimName: String?) -> Unit
) {
    var errorMessage by remember { mutableStateOf("") }
    var thiefPlayerName by remember { mutableStateOf("") }
    var victimPlayerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Способность Вора ===",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // поле для никнейма игрока, играющего за вора
        OutlinedTextField(
            value = thiefPlayerName,
            onValueChange = {
                thiefPlayerName = it
                errorMessage = "" // Сбрасываем ошибку при вводе
            },
            label = { Text("Никнейм Вора") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // поле для никнейма игрока-жертвы
        OutlinedTextField(
            value = victimPlayerName,
            onValueChange = {
                victimPlayerName = it
                errorMessage = ""
            },
            label = { Text("Никнейм Жертвы (если вор назвал персонажа в раунде)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка подтверждения кражи
            Button(
                onClick = {
                    val trimmedThief = thiefPlayerName.trim()
                    val trimmedVictim = victimPlayerName.trim()

                    val thiefExists = playersList.any { it.name.equals(trimmedThief, ignoreCase = true) }
                    val victimExists = playersList.any { it.name.equals(trimmedVictim, ignoreCase = true) }

                    when {
                        trimmedThief.isBlank() -> {
                            errorMessage = "Никнейм Вора не может быть пустым."
                        }
                        !thiefExists -> {
                            errorMessage = "Игрок с ником '$trimmedThief' не найден."
                        }
                        trimmedVictim.isBlank() -> {
                            errorMessage = "Никнейм Жертвы не может быть пустым."
                        }
                        !victimExists -> {
                            errorMessage = "Игрок с ником '$trimmedVictim' не найден."
                        }
                        trimmedThief.equals(trimmedVictim, ignoreCase = true) -> {
                            errorMessage = "Вор не может ограбить самого себя!"
                        }
                        else -> {
                            errorMessage = ""
                            onAnswerConfirmed(true, trimmedThief, trimmedVictim)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Применить кражу", color = Color.White, style = MaterialTheme.typography.button)
            }

            // Кнопка пропуска (Вор выбрал отсутствующего персонажа)
            Button(
                onClick = {
                    onAnswerConfirmed(false, null, null)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Вор назвал отсутствующего персонажа (Пропустить)", color = Color.White, style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
fun SorcererSpecRule(
    playersList: List<Player>,
    onAnswerConfirmed: (isApplied: Boolean, sorcererName: String?, victimName: String?) -> Unit
) {
    var errorMessage by remember { mutableStateOf("") }
    var sorcererPlayerName by remember { mutableStateOf("") }
    var victimPlayerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Способность Чародея ===",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        // поле ввода для игрока, играющего за ЧАРОДЕЯ
        OutlinedTextField(
            value = sorcererPlayerName,
            onValueChange = {
                sorcererPlayerName = it
                errorMessage = ""
            },
            label = { Text("Никнейм Чародея") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // поле ввода для игрока-ЖЕРТВЫ
        OutlinedTextField(
            value = victimPlayerName,
            onValueChange = {
                victimPlayerName = it
                errorMessage = ""
            },
            label = { Text("Никнейм Жертвы") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // кнопка подтверждения обмена с игроком
            Button(
                onClick = {
                    val trimmedSorcerer = sorcererPlayerName.trim()
                    val trimmedVictim = victimPlayerName.trim()

                    val sorcererExists = playersList.any { it.name.equals(trimmedSorcerer, ignoreCase = true) }
                    val victimExists = playersList.any { it.name.equals(trimmedVictim, ignoreCase = true) }

                    when {
                        trimmedSorcerer.isBlank() -> {
                            errorMessage = "Никнейм Чародея не может быть пустым."
                        }
                        !sorcererExists -> {
                            errorMessage = "Игрок с ником '$trimmedSorcerer' не найден."
                        }
                        trimmedVictim.isBlank() -> {
                            errorMessage = "Никнейм Жертвы не может быть пустым."
                        }
                        !victimExists -> {
                            errorMessage = "Игрок с ником '$trimmedVictim' не найден."
                        }
                        trimmedSorcerer.equals(trimmedVictim, ignoreCase = true) -> {
                            errorMessage = "Чародей не может совершить обмен с самим собой!"
                        }
                        else -> {
                            errorMessage = ""
                            onAnswerConfirmed(true, trimmedSorcerer, trimmedVictim)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Обменять с другим игроком", color = Color.White, style = MaterialTheme.typography.button)
            }

            //кнопка полной замены руки
            Button(
                onClick = {
                    onAnswerConfirmed(false, null, null)
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Обменять все карты на карты из колоды", color = Color.White, style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
fun GetCardsOrMoney(player: Player?, onAnswerConfirmed: (answer: Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Выберите монеты или карты ===",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка "Монеты"
            Button(
                onClick = { onAnswerConfirmed(true) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Монеты", color = Color.White, style = MaterialTheme.typography.button)
            }

            // Кнопка "Карты"
            Button(
                onClick = { onAnswerConfirmed(false) },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF44336))
            ) {
                Text("Карты", color = Color.White, style = MaterialTheme.typography.button)
            }
        }
    }

}

@Composable
fun BuildQuarter(player: Player?, onAnswerConfirmed: (Boolean) -> Unit) {
    var errorMessage by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Введите название квартала, который строите: ===",
            style = MaterialTheme.typography.h5
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = district,
            onValueChange = {
                district = it
                errorMessage = ""
            },
            label = { Text("Название карты квартала") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val trimmedCard = district.trim()
                    val foundCard = player?.hand?.find { it.name.equals(trimmedCard, ignoreCase = true) }

                    when {
                        trimmedCard.isBlank() -> {
                            errorMessage = "Название карты не может быть пустым."
                        }
                        foundCard == null -> {
                            errorMessage = "Такой карты нет у вас в руке!"
                        }
                        (player.gold) < (foundCard.cost) -> {
                            errorMessage = "У вас недостаточно золота"
                        }
                        else -> {
                            player.build(foundCard)
                            onAnswerConfirmed(true)
                        }
                    }
                }
            ) {
                Text("Построить квартал")
            }

            OutlinedButton(
                onClick = { onAnswerConfirmed(false) }
            ) {
                Text("Не строить")
            }
        }
    }
}

@Composable
fun GetCards (initialDistricts: Map<IDistrict, Int>, player: Player?, amount: Int, onCardsDistributed: (Player?) -> Unit) {
    var cardNameInput by remember { mutableStateOf("") }

    var currentCardsList by remember { mutableStateOf(listOf<IDistrict>()) }

    val districtCards = remember { mutableStateOf(QuarterPool(initialDistricts)) }

    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Введите ${amount} полученных карт ===",
            style = MaterialTheme.typography.h5
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text("Введено карт: ${currentCardsList.size} из ${amount}")
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
            items(currentCardsList) { card ->
                Text("• ${card.name}", style = MaterialTheme.typography.body1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = cardNameInput,
            onValueChange = {
                cardNameInput = it
                errorMessage = ""
            },
            label = { Text("Название карты квартала") },
            enabled = currentCardsList.size < amount,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val trimmedCard = cardNameInput.trim()
                val foundCard = districtCards.value.findDistrictCard(trimmedCard)


                when {
                    trimmedCard.isBlank() -> {
                        errorMessage = "Название карты не может быть пустым."
                    }
                    foundCard == null -> {
                        errorMessage = "Такой карты нет в колоде Цитаделей!"
                    }
                    else -> {
                        currentCardsList = currentCardsList + foundCard
                        districtCards.value.drawCard(foundCard)
                        cardNameInput = ""
                        errorMessage = ""
                    }
                }
            },
            enabled = currentCardsList.size < amount
        ) {
            Text("Записать карту")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (currentCardsList.size == amount) {
                    for (card in currentCardsList) {
                        player?.addToHand(card)
                    }
                    onCardsDistributed(player)
                } else {
                    errorMessage = "Необходимо ввести ровно $amount карт!"
                }
            },
            enabled = currentCardsList.size == amount,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF4CAF50)
            )
        ) {
            Text("Подтвердить", color = Color.White)
        }
    }
}

@Composable
fun WhoPlaysFor(
    playersList: List<Player>,
    character: ICharacter?,
    onAnswerConfirmed: (player: Player) -> Unit
) {
    var errorMessage by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "=== Введите никнейм игрока, играющего за ${character?.name} ===",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputName,
            onValueChange = {
                inputName = it
                errorMessage = ""
            },
            label = { Text("Никнейм игрока") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.body2
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val trimmedNickname = inputName.trim()

                    val foundPlayer = playersList.find { it.name.equals(trimmedNickname, ignoreCase = true) }

                    when {
                        trimmedNickname.isBlank() -> {
                            errorMessage = "Никнейм Игрока не может быть пустым."
                        }
                        foundPlayer == null -> {
                            errorMessage = "Игрок с ником '$trimmedNickname' не найден."
                        }
                        else -> {
                            errorMessage = ""
                            onAnswerConfirmed(foundPlayer)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Подтвердить Игрока", color = Color.White, style = MaterialTheme.typography.button)
            }
        }
    }
}

@Composable
fun results(playersList: List<Player>, onAnswerConfirmed: (Boolean) -> Unit) {

}
