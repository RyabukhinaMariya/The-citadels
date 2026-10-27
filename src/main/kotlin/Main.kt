import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import data.MoveType
import engine.GameState

fun main() = application {
    DatabaseManager.init()
    val gameSession = remember { GameSession() }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Цитадели — Desktop GUI"
    ) {
        CitadelApp(gameSession)
    }
}

private object AppColors {
    val Background = Color(0xFF1E1E2C)
    val CardBackground = Color(0xFF2D2D44)
    val ButtonSecondary = Color(0xFF5C6BC0)
}

@Composable
private fun GameScreen(
    state: GameState,
    session: GameSession,
    onShowHistory: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameTopBar(
            message = state.message,
            onShowHistory = onShowHistory
        )
        Spacer(modifier = Modifier.height(16.dp))
        PhaseRouter(state = state, session = session)
    }
}

@Composable
private fun GameTopBar(message: String, onShowHistory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MessageBanner(message = message, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onShowHistory,
            colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.ButtonSecondary)
        ) {
            Text("История", color = Color.White)
        }
    }
}

@Composable
private fun PhaseRouter(state: GameState, session: GameSession) {
    when (state.phase) {
        GamePhase.SETUP_PLAYERS -> SetupScreen(session, state.players)

        GamePhase.CHARACTER_CALL -> CharacterCallScreen(
            session = session,
            players = state.players,
            characterName = state.activeCharacterName
        )

        GamePhase.SPECIAL_ABILITY -> AbilityInputScreen(
            session = session,
            characterName = domain.GameCharacters.getByOrder(state.currentCharacterIndex)?.name,
            characterRank = state.currentCharacterIndex
        )

        GamePhase.ACTION_CHOICE -> ActionChoiceScreen(session, state.activePlayer)
        GamePhase.BUILD_CHOICE -> BuildAndAbilityScreen(session, state.activePlayer)
        GamePhase.GAME_OVER -> GameOverScreen(state.message)
        GamePhase.SELECT_CARD -> SelectCardScreen { name -> session.confirmSelectedCard(name) }
        GamePhase.LEADERBOARD -> LeaderboardScreen(session)
        else -> error("Unknown phase")
    }
}

@Composable
private fun MessageBanner(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(AppColors.CardBackground, RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

@Composable
private fun CitadelApp(session: GameSession) {
    val state by session.gameState.collectAsState()
    val historyVersion by session.historyVersion.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    val history = remember(historyVersion) { session.moveHistory }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.Background
        ) {
            if (showHistory) {
                MoveHistoryScreen(entries = history, onClose = { showHistory = false })
            } else {
                GameScreen(
                    state = state,
                    session = session,
                    onShowHistory = { showHistory = true }
                )
            }
        }
    }
}

private object SetupDimens {
    val SectionSpacing = 12.dp
    val ListSpacing = 20.dp
    val ButtonSpacing = 12.dp
    val LeaderboardBottomPadding = 20.dp
}

// 1. Register screen
@Composable
fun SetupScreen(session: GameSession, players: List<IPlayer>) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        LeaderboardButton(onClick = session::openLeaderboard)

        SetupHeader()

        var nameInput by remember { mutableStateOf("") }

        PlayerNameField(
            value = nameInput,
            onValueChange = { nameInput = it }
        )

        Spacer(modifier = Modifier.height(SetupDimens.SectionSpacing))

        SetupActions(
            nameInput = nameInput,
            playersCount = players.size,
            onAddPlayer = {
                session.addPlayer(nameInput)
                nameInput = ""
            },
            onStartGame = session::startGame
        )

        Spacer(modifier = Modifier.height(SetupDimens.SectionSpacing))

        RegisteredPlayersList(players)
    }
}

@Composable
private fun LeaderboardButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF7E57C2)),
        modifier = Modifier.padding(bottom = SetupDimens.LeaderboardBottomPadding)
    ) {
        Text("Результаты игр / Рейтинг", color = Color.White)
    }
}

@Composable
private fun SetupHeader() {
    Text(
        "Регистрация участников (4-7 игроков)",
        color = Color.LightGray,
        fontSize = 20.sp
    )
    Spacer(modifier = Modifier.height(SetupDimens.SectionSpacing))
}

@Composable
private fun PlayerNameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Имя игрока") },
        singleLine = true,
        colors = setupTextFieldColors()
    )
}

@Composable
private fun SetupActions(
    nameInput: String,
    playersCount: Int,
    onAddPlayer: () -> Unit,
    onStartGame: () -> Unit
) {
    Row {
        AddPlayerButton(onClick = onAddPlayer)
        Spacer(modifier = Modifier.width(SetupDimens.ButtonSpacing))
        if (playersCount >= 4) {
            StartGameButton(playersCount = playersCount, onClick = onStartGame)
        }
    }
}

@Composable
private fun AddPlayerButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50))
    ) {
        Text("Добавить", color = Color.White)
    }
}

@Composable
private fun StartGameButton(playersCount: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFF9800))
    ) {
        Text("Начать игру ($playersCount)", color = Color.White)
    }
}

@Composable
private fun RegisteredPlayersList(players: List<IPlayer>) {
    Text("Зарегистрированы:", color = Color.Gray)
    players.forEach { player -> PlayerRow(player.name) }
}

@Composable
private fun PlayerRow(name: String) {
    Text("• $name", color = Color.White, fontSize = 16.sp)
}

@Composable
private fun setupTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Color.White,
    focusedBorderColor = Color(0xFFFFB74D),
    unfocusedBorderColor = Color.Gray
)

@Composable
fun LeaderboardScreen(session: GameSession) {
    val leaderboard = remember { DatabaseManager.getLeaderboard() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Таблица лидеров",
            color = Color(0xFFFFD54F),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (leaderboard.isEmpty()) {
            Text("История игр пока пуста", color = Color.Gray, fontSize = 16.sp)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF37474F), RoundedCornerShape(4.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Игрок", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                Text("Сыграно", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Победы", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(leaderboard) { stats ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF263238), RoundedCornerShape(4.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stats.name, color = Color.White, modifier = Modifier.weight(2f))
                        Text("${stats.gamesPlayed}", color = Color.LightGray, modifier = Modifier.weight(1f))
                        Text("${stats.wins}", color = Color(0xFFFFD700), modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        Button(
            onClick = { session.backToSetup() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF5C6BC0)),
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            Text("Назад в меню", color = Color.White)
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
            Text("Золото: ${activePlayer?.gold}", color = Color(0xFFFFD700), fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { session.triggerActiveCharacterAbility() },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFAB47BC)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Активировать способность персонажа", color = Color.White, fontSize = 16.sp)
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
            Text("Цена: ${district.cost}", color = Color(0xFFFFD700))
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
        Text("Игра окончена!", color = Color(0xFFFFD700), fontSize = 28.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(results, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun SelectCardScreen(onCardSelected: (String) -> Unit) {
    var cardNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Card(
            backgroundColor = Color(0xFF2D2D44),
            shape = RoundedCornerShape(12.dp),
            elevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.6f).padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                SelectCardHeader()
                Spacer(modifier = Modifier.height(20.dp))
                CardNameField(
                    value = cardNameInput,
                    onValueChange = {
                        cardNameInput = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    }
                )
                ErrorMessage(errorMessage)
                Spacer(modifier = Modifier.height(24.dp))
                ConfirmCardButton(
                    onClick = {
                        errorMessage = handleCardSubmit(cardNameInput, onCardSelected)
                        if (errorMessage.isEmpty()) cardNameInput = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectCardHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    }
}

@Composable
private fun CardNameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Название карты") },
        singleLine = true,
        colors = selectCardFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ErrorMessage(message: String) {
    if (message.isEmpty()) return
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = message, color = Color(0xFFE53935), fontSize = 12.sp)
}

@Composable
private fun ConfirmCardButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
        modifier = Modifier.fillMaxWidth().height(48.dp),
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

@Composable
private fun selectCardFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Color.White,
    focusedBorderColor = Color(0xFFFFB74D),
    unfocusedBorderColor = Color.Gray,
    focusedLabelColor = Color(0xFFFFB74D),
    cursorColor = Color(0xFFFFB74D)
)

/**
 * Возвращает текст ошибки или пустую строку, если всё ок.
 * Вынесено из composable — легко тестируется.
 */
private fun handleCardSubmit(
    input: String,
    onCardSelected: (String) -> Unit
): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return "Название карты не может быть пустым!"
    onCardSelected(trimmed)
    return ""
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
        modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E2C)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f).padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF2D2D44),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AbilityHeader(characterName)
                AbilityInputFields(
                    characterRank = characterRank,
                    targetInput = targetInput,
                    onTargetChange = { targetInput = it },
                    extraInput = extraInput,
                    onExtraChange = { extraInput = it }
                )
                AbilityActionButtons(
                    onSkip = { session.skipAbility() },
                    onConfirm = {
                        submitAbility(session, characterRank, targetInput, extraInput)
                    }
                )
            }
        }
    }
}

@Composable
private fun AbilityHeader(characterName: String?) {
    Text(
        "Способность персонажа",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    characterName?.let {
        Text("Вы играете за: $it", fontSize = 16.sp, color = Color(0xFFFFD700))
    }
}

@Composable
private fun AbilityInputFields(
    characterRank: Int,
    targetInput: String,
    onTargetChange: (String) -> Unit,
    extraInput: String,
    onExtraChange: (String) -> Unit
) {
    when (characterRank) {
        3 -> SorcererFields(targetInput, onTargetChange)
        1 -> AssassinFields(targetInput, onTargetChange)
        2 -> ThiefFields(targetInput, onTargetChange)
        8 -> WarlordFields(targetInput, onTargetChange, extraInput, onExtraChange)
        else -> Text("У этой роли нет активной цели.", color = Color.Gray)
    }
}

@Composable
private fun SorcererFields(value: String, onChange: (String) -> Unit) {
    Text("Обмен картами с другим игроком", color = Color.LightGray, fontSize = 14.sp)
    AbilityTextField(
        value = value,
        onValueChange = onChange,
        label = "На кого вы воздействуете",
        placeholder = "Введите ник игрока"
    )
}

@Composable
private fun AssassinFields(value: String, onChange: (String) -> Unit) {
    AbilityTextField(
        value = value,
        onValueChange = onChange,
        label = "Ранг персонажа для убийства (2-8) или 0"
    )
}

@Composable
private fun ThiefFields(value: String, onChange: (String) -> Unit) {
    AbilityTextField(
        value = value,
        onValueChange = onChange,
        label = "Ранг персонажа для ограбления (2-8)"
    )
}

@Composable
private fun WarlordFields(
    targetValue: String,
    onTargetChange: (String) -> Unit,
    extraValue: String,
    onExtraChange: (String) -> Unit
) {
    AbilityTextField(
        value = targetValue,
        onValueChange = onTargetChange,
        label = "На кого вы воздействуете (ник игрока)"
    )
    AbilityTextField(
        value = extraValue,
        onValueChange = onExtraChange,
        label = "Название здания для разрушения"
    )
}

@Composable
private fun AbilityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        colors = textFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AbilityActionButtons(
    onSkip: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onSkip,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF757575)),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Text("Пропустить", color = Color.White, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Text("Подтвердить", color = Color.White, fontSize = 16.sp)
        }
    }
}

private fun submitAbility(
    session: GameSession,
    characterRank: Int,
    targetInput: String,
    extraInput: String
) {
    when (characterRank) {
        3 -> if (targetInput.isNotBlank()) session.applySorcererSwap(targetInput.trim())
        1 -> targetInput.toIntOrNull()?.let { session.applyAssassinAbility(it) }
        2 -> targetInput.toIntOrNull()?.let { session.applyThiefAbility(it) }
        8 -> if (targetInput.isNotBlank() && extraInput.isNotBlank()) {
            session.applyWarlordDestroy(targetInput.trim(), extraInput.trim())
        }
        else -> session.skipAbility()
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

@Composable
fun MoveHistoryScreen(
    entries: List<data.GameLogEntry>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "История ходов (${entries.size})",
                color = Color(0xFFFFD54F),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935))
            ) {
                Text("Закрыть", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (entries.isEmpty()) {
            Text("Ходов пока не было", color = Color.Gray)
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(entries) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: data.GameLogEntry) {
    val color = when (entry.type) {
        MoveType.PLAYER_KILLED, MoveType.DISTRICT_DESTROYED -> Color(0xFFE53935)
        MoveType.PLAYER_ROBBED -> Color(0xFFFF7043)
        MoveType.DISTRICT_BUILT -> Color(0xFF66BB6A)
        MoveType.GOLD_TAKEN, MoveType.CARD_DRAWN -> Color(0xFFFFD54F)
        MoveType.GAME_OVER -> Color(0xFFAB47BC)
        MoveType.CHARACTER_CALLED, MoveType.CHARACTER_SKIPPED -> Color(0xFF42A5F5)
        else -> Color.LightGray
    }

    Card(
        backgroundColor = Color(0xFF2D2D44),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Р${entry.round}",
                color = Color(0xFF9575CD),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            Text(
                entry.formatTime(),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.width(70.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.description, color = color, fontSize = 14.sp)
                if (entry.characterName != null || entry.playerName != null) {
                    Text(
                        listOfNotNull(entry.characterName, entry.playerName)
                            .joinToString(" · "),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
