package com.programmersbox.blackjackcompose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.accompanist.flowlayout.FlowRow
import com.programmersbox.blackjackcompose.ui.theme.BlackjackComposeTheme
import com.programmersbox.funutils.cards.Card
import com.programmersbox.funutils.cards.Deck
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @ExperimentalMaterialApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlackjackComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Blackjack()
                }
            }
        }
    }
}

private fun List<Card>.toSum() = sortedByDescending { if (it.value > 10) 10 else it.value }
    .fold(0) { v, c ->
        v + if (c.value == 1 && v + 11 < 22) {
            11
        } else if (c.value == 1) {
            1
        } else {
            if (c.value > 10) 10 else c.value
        }
    }

class BlackjackStats {
    var winCount by mutableStateOf(0)
    var loseCount by mutableStateOf(0)
    var drawCount by mutableStateOf(0)
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore("blackjack")
val WIN_COUNT = intPreferencesKey("wins")
val LOSE_COUNT = intPreferencesKey("loses")
val DRAW_COUNT = intPreferencesKey("draws")

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun Blackjack() {
    val playerHand = remember { mutableStateListOf<Card>() }
    val dealerHand = remember { mutableStateListOf<Card>() }
    var cardCount by remember { mutableStateOf(52) }

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    val dataStore = LocalContext.current.dataStore
    val totalWins by dataStore.data.map { it[WIN_COUNT] ?: 0 }.collectAsState(initial = 0)
    val totalLoses by dataStore.data.map { it[LOSE_COUNT] ?: 0 }.collectAsState(initial = 0)
    val totalDraws by dataStore.data.map { it[DRAW_COUNT] ?: 0 }.collectAsState(initial = 0)

    fun updateTotalStat(key: Preferences.Key<Int>, total: Int) = scope.launch { dataStore.edit { it[key] = total + 1 } }

    val stats = remember { BlackjackStats() }
    var playing by remember { mutableStateOf(true) }

    val deck = remember {
        val d = Deck.defaultDeck()
        d.shuffle()
        d.addDeckListener {
            onDraw { _, size ->
                if (size == 0) {
                    d.addDeck(Deck.defaultDeck())
                    d.shuffle()
                }
                cardCount = size
            }
        }
        d
    }

    fun winCheck() {
        val pSum = playerHand.toSum()
        val dSum = dealerHand.toSum()

        val state = when {
            pSum > 21 && dSum <= 21 -> {
                stats.loseCount++
                updateTotalStat(LOSE_COUNT, totalLoses)
                "Lose"
            }
            dSum > 21 && pSum <= 21 -> {
                stats.winCount++
                updateTotalStat(WIN_COUNT, totalWins)
                "Win"
            }
            pSum in (dSum + 1)..21 -> {
                stats.winCount++
                updateTotalStat(WIN_COUNT, totalWins)
                "Win"
            }
            dSum in (pSum + 1)..21 -> {
                stats.loseCount++
                updateTotalStat(LOSE_COUNT, totalLoses)
                "Lose"
            }
            dSum == pSum && dSum <= 21 && pSum <= 21 -> {
                stats.drawCount++
                updateTotalStat(DRAW_COUNT, totalDraws)
                "Got a Draw"
            }
            else -> {
                stats.drawCount++
                updateTotalStat(DRAW_COUNT, totalDraws)
                "Got a Draw"
            }
        }

        scope.launch { scaffoldState.snackbarHostState.showSnackbar("You $state", duration = SnackbarDuration.Short) }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { scaffoldState.drawerState.open() } }) { Icon(Icons.Default.Menu, null) }
                },
                title = { Text("Dealer has: ${dealerHand.toSum()}") },
                actions = { Text("$cardCount card(s) left") }
            )
        },
        bottomBar = { BottomAppBar { Text("Player has: ${playerHand.toSum()}", style = MaterialTheme.typography.h6) } },
        drawerContent = {
            Scaffold(topBar = { TopAppBar(title = { Text("Stats") }) }) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .padding(it),
                ) {
                    val typography = MaterialTheme.typography.body1
                    Text("Times won: ${stats.winCount}", style = typography)
                    Text("Times lost: ${stats.loseCount}", style = typography)
                    Text("Times drawn: ${stats.drawCount}", style = typography)
                    Divider()
                    Text("Total Times won: $totalWins", style = typography)
                    Text("Total Times lost: $totalLoses", style = typography)
                    Text("Total Times drawn: $totalDraws", style = typography)
                    Button(
                        onClick = {
                            updateTotalStat(WIN_COUNT, -1)
                            updateTotalStat(LOSE_COUNT, -1)
                            updateTotalStat(DRAW_COUNT, -1)
                        }
                    ) { Text("Reset Saved Stats", style = MaterialTheme.typography.button) }
                }
            }
        }
    ) { p ->
        Column(
            modifier = Modifier
                .padding(p)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Hand(dealerHand)

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Button(
                    onClick = {
                        playerHand.clear()
                        dealerHand.clear()
                        dealerHand.addAll(deck.draw(2))
                        playerHand.addAll(deck.draw(2))
                        playing = true
                    }
                ) { Text("Play Again", style = MaterialTheme.typography.button) }

                Button(
                    onClick = {
                        playing = false
                        scope.launch {
                            while (dealerHand.toSum() <= 17) {
                                dealerHand.add(deck.draw())
                                delay(500)
                            }
                            winCheck()
                        }
                    },
                    enabled = playing
                ) { Text("Stay", style = MaterialTheme.typography.button) }

                Button(
                    onClick = {
                        playerHand.add(deck.draw())
                        if (playerHand.toSum() > 21) {
                            playing = false
                            winCheck()
                        }
                    },
                    enabled = playerHand.toSum() <= 21 && playing
                ) { Text("Hit", style = MaterialTheme.typography.button) }
            }

            Hand(playerHand)
        }
    }

    dealerHand.addAll(deck.draw(2))
    playerHand.addAll(deck.draw(2))
}

@ExperimentalMaterialApi
@Composable
fun Hand(h: SnapshotStateList<Card>) = LazyRow {
    items(h) {
        PlayingCard(
            card = it,
            modifier = Modifier.padding(5.dp)
        )
    }
}

@ExperimentalMaterialApi
@Composable
fun PlayingCard(card: Card, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(7.dp),
        modifier = Modifier
            .size(100.dp, 150.dp)
            .then(modifier),
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = card.toSymbolString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                textAlign = TextAlign.Start
            )
            FlowRow(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { repeat(card.value) { Text(text = card.suit.unicodeSymbol, textAlign = TextAlign.Center) } }
            Text(
                text = card.toSymbolString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp),
                textAlign = TextAlign.End
            )
        }
    }
}
