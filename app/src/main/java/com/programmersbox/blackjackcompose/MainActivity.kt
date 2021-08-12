package com.programmersbox.blackjackcompose

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.programmersbox.blackjackcompose.ui.theme.BlackjackComposeTheme
import com.programmersbox.funutils.cards.Card
import com.programmersbox.funutils.cards.Deck
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultPreview() {
    BlackjackComposeTheme {
        Blackjack()
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DefaultPreview2() {
    BlackjackComposeTheme {
        //Greeting("Android")

        val deck = Deck.defaultDeck()

        LazyVerticalGrid(
            cells = GridCells.Adaptive(100.dp)
        ) {
            items(deck.draw(14)) {
                PlayingCard(
                    card = it,
                    modifier = Modifier.padding(5.dp)
                )
            }
        }

        /*Column {
            PlayingCard(card = Card(1, Suit.SPADES))
            Spacer(Modifier.height(5.dp))
            PlayingCard(card = Card(10, Suit.SPADES))
        }*/

    }
}

class BlackjackStats {
    var winCount by mutableStateOf(0)
    var loseCount by mutableStateOf(0)
    var drawCount by mutableStateOf(0)
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun Blackjack() {
    val playerHand = remember { mutableStateListOf<Card>() }
    val dealerHand = remember { mutableStateListOf<Card>() }
    var cardCount by remember { mutableStateOf(52) }

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

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    fun winCheck() {
        val pSum = playerHand.toSum()
        val dSum = dealerHand.toSum()

        val state = when {
            pSum > 21 && dSum <= 21 -> {
                stats.loseCount++
                "Lose"
            }
            dSum > 21 && pSum <= 21 -> {
                stats.winCount++
                "Win"
            }
            pSum in (dSum + 1)..21 -> {
                stats.winCount++
                "Win"
            }
            dSum in (pSum + 1)..21 -> {
                stats.loseCount++
                "Lose"
            }
            dSum == pSum && dSum <= 21 && pSum <= 21 -> {
                stats.drawCount++
                "Got a Draw"
            }
            else -> "Got a Draw"
        }

        scope.launch { scaffoldState.snackbarHostState.showSnackbar("You $state", duration = SnackbarDuration.Short) }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (scaffoldState.drawerState.isClosed) scaffoldState.drawerState.open()
                                else scaffoldState.drawerState.close()
                            }
                        }
                    ) { Icon(Icons.Default.Menu, null) }
                },
                title = { Text("Dealer has: ${dealerHand.toSum()}") },
                actions = { Text("$cardCount card(s) left") }
            )
        },
        bottomBar = {
            BottomAppBar { Text("Player has: ${playerHand.toSum()}", style = MaterialTheme.typography.h6) }
        },
        drawerContent = {
            Scaffold(topBar = { TopAppBar(title = { Text("Stats") }) }) {
                LazyColumn(
                    modifier = Modifier.padding(5.dp),
                    contentPadding = it
                ) {
                    item {
                        val typography = MaterialTheme.typography.h5
                        Text("Times won: ${stats.winCount}", style = typography)
                        Text("Times lost: ${stats.loseCount}", style = typography)
                        Text("Times drawn: ${stats.drawCount}", style = typography)
                    }
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

            LazyRow {
                items(dealerHand) {
                    PlayingCard(
                        card = it,
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }

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

            LazyRow {
                items(playerHand) {
                    PlayingCard(
                        card = it,
                        onClick = { playerHand.add(deck.draw()) },
                        modifier = Modifier.padding(5.dp)
                    )
                }
            }

        }
    }

    dealerHand.addAll(deck.draw(2))
    playerHand.addAll(deck.draw(2))
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
