import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import engine.GamePhase
import engine.GameSession
import domain.IPlayer
import domain.IDistrict
import database.DatabaseManager

fun main() = application {
    val gameSession = remember { GameSession() }
    DatabaseManager.init()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Цитадели — Desktop GUI"
    ) {
        val state by gameSession.gameState.collectAsState()

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1E1E2C)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2D2D44), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    when (state.phase) {
                        GamePhase.SETUP_PLAYERS -> SetupScreen(gameSession, state.players)
                        GamePhase.CHARACTER_CALL -> CharacterCallScreen(gameSession, state.players, state.activeCharacterName)
                        GamePhase.SPECIAL_ABILITY -> {
                            val currentRank = state.currentCharacterIndex
                            AbilityInputScreen(
                                session = gameSession,
                                characterName = domain.GameCharacters.getByOrder(currentRank)?.name,
                                characterRank = currentRank
                            )
                        }
                        GamePhase.ACTION_CHOICE -> ActionChoiceScreen(gameSession, state.activePlayer)
                        GamePhase.BUILD_CHOICE -> BuildAndAbilityScreen(gameSession, state.activePlayer)
                        GamePhase.GAME_OVER -> GameOverScreen(state.message)
                        GamePhase.SELECT_CARD -> SelectCardScreen { selectedCardName -> gameSession.confirmSelectedCard(selectedCardName) }
                        else -> exitApplication()
                    }
                }
            }
        }
    }
}

// 1. Register screen
@Composable
fun SetupScreen(session: GameSession, players: List<IPlayer>) {
    var nameInput by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Регистрация участников (4-7 игроков)", color = Color.LightGray, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Имя игрока") },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.White,
                focusedBorderColor = Color(0xFFFFB74D),
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            Button(
                onClick = {
                    session.addPlayer(nameInput)
                    nameInput = ""
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
            ) {
                Text("Добавить", color = Color.White)
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (players.size >= 4) {
                Button(
                    onClick = { session.startGame() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800))
                ) {
                    Text("Начать игру (${players.size})", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("Зарегистрированы:", color = Color.Gray)
        players.forEach { p ->
            Text("• ${p.name}", color = Color.White, fontSize = 16.sp)
        }
    }
}

// 2. Character choose screen
@Composable
fun CharacterCallScreen(session: GameSession, players: List<IPlayer>, characterName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Вызывается роль: $characterName", color = Color(0xFFFFD54F), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Укажите, у кого из игроков карта этого персонажа:", color = Color.LightGray)

        Spacer(modifier = Modifier.height(20.dp))

        players.forEach { player ->
            Button(
                onClick = { session.confirmCharacterPlayer(player.id) },
                modifier = Modifier.fillMaxWidth(0.6f).padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3F51B5))
            ) {
                Text(player.name, color = Color.White, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { session.confirmCharacterPlayer(null) },
            modifier = Modifier.fillMaxWidth(0.6f),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
        ) {
            Text("Роль сброшена / Никто не брал", color = Color.White)
        }
    }
}

// 3. action choose screen
@Composable
fun ActionChoiceScreen(session: GameSession, activePlayer: IPlayer?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Ход игрока: ${activePlayer?.name}", color = Color.White, fontSize = 22.sp)
        Text("Золото: ${activePlayer?.gold}", color = Color(0xFFFFD700), fontSize = 18.sp)

        Spacer(modifier = Modifier.height(30.dp))

        Row {
            Button(
                onClick = { session.takeGold() },
                modifier = Modifier.size(160.dp, 80.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFB300))
            ) {
                Text("Взять\n2 золотых", fontSize = 18.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.width(20.dp))

            Button(
                onClick = { session.startSelectCardPhase() }, // Переходим на экран выбора карты
                modifier = Modifier.size(160.dp, 80.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF29B6F6))
            ) {
                Text("Тянуть\nкарту", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

// 4. build and ability screen
@Composable
fun BuildAndAbilityScreen(
    session: GameSession,
    activePlayer: IPlayer?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ходит: ${activePlayer?.name}", color = Color.White, fontSize = 20.sp)
            Text("Золото: ${activePlayer?.gold} 🪙", color = Color(0xFFFFD700), fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { session.triggerActiveCharacterAbility() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFAB47BC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("✨ Активировать способность персонажа", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Ваши карты в руке (Кликните для постройки):", color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(activePlayer?.hand ?: emptyList()) { district ->
                DistrictCardView(district) {
                    session.buildDistrict(district)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { session.buildDistrict(null) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5C6BC0))
        ) {
            Text("Завершить ход", color = Color.White, fontSize = 18.sp)
        }
    }

}

// cards
@Composable
fun DistrictCardView(district: IDistrict, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(120.dp, 160.dp)
            .clickable { onClick() }
            .border(1.dp, Color.White, RoundedCornerShape(8.dp)),
        backgroundColor = Color(0xFF37474F)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(district.name, color = Color.White, fontWeight = FontWeight.Bold)
            Text("Цена: ${district.cost} 🪙", color = Color(0xFFFFD700))
            Text("Цвет: ${district.color}", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

// Results screen
@Composable
fun GameOverScreen(results: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("🏆 Игра окончена!", color = Color(0xFFFFD700), fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(results, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun SelectCardScreen(
    onCardSelected: (String) -> Unit
) {
    var cardNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Card(
            backgroundColor = Color(0xFF2D2D44),
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Выбор карты",
                    color = Color(0xFFFFD54F),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Введите точное название карты квартала:",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // card name field
                OutlinedTextField(
                    value = cardNameInput,
                    onValueChange = {
                        cardNameInput = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    label = { Text("Название карты") },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        focusedBorderColor = Color(0xFFFFB74D),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFFFFB74D),
                        cursorColor = Color(0xFFFFB74D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFE53935),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                //apply button
                Button(
                    onClick = {
                        if (cardNameInput.trim().isEmpty()) {
                            errorMessage = "Название карты не может быть пустым!"
                        } else {
                            onCardSelected(cardNameInput.trim())
                            cardNameInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Подтвердить выбор",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AbilityInputScreen(
    session: GameSession,
    characterName: String?,
    characterRank: Int
) {
    var targetInput by remember { mutableStateOf("") }
    var extraInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2C)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF2D2D44),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Способность персонажа",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (characterName != null) {
                    Text(
                        text = "Вы играете за: $characterName",
                        fontSize = 16.sp,
                        color = Color(0xFFFFD700)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Динамический интерфейс в зависимости от роли
                when (characterRank) {
                    3 -> { // Чародей
                        Text(
                            text = "Обмен картами с другим игроком",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("На кого вы воздействуете") },
                            placeholder = { Text("Введите ник игрока") },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    1 -> { // Ассасин
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("Ранг персонажа для убийства (2-8) или 0") },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> { // Вор
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("Ранг персонажа для ограбления (2-8)") },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    8 -> { // Кондотьер
                        OutlinedTextField(
                            value = targetInput,
                            onValueChange = { targetInput = it },
                            label = { Text("На кого вы воздействуете (ник игрока)") },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = extraInput,
                            onValueChange = { extraInput = it },
                            label = { Text("Название здания для разрушения") },
                            singleLine = true,
                            colors = textFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Text("У этой роли нет активной цели.", color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { session.skipAbility() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF757575)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Пропустить", color = Color.White, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            when (characterRank) {
                                3 -> if (targetInput.isNotBlank()) session.applySorcererSwap(targetInput.trim())
                                1 -> targetInput.toIntOrNull()?.let { session.applyAssassinAbility(it) }
                                2 -> targetInput.toIntOrNull()?.let { session.applyThiefAbility(it) }
                                8 -> if (targetInput.isNotBlank() && extraInput.isNotBlank()) {
                                    session.applyWarlordDestroy(targetInput.trim(), extraInput.trim())
                                }
                                else -> session.skipAbility()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Подтвердить", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Color.White,
    focusedBorderColor = Color(0xFF4CAF50),
    unfocusedBorderColor = Color.Gray,
    cursorColor = Color(0xFF4CAF50),
    focusedLabelColor = Color(0xFF4CAF50)
)
