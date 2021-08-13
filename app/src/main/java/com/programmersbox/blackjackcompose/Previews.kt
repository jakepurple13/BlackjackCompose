package com.programmersbox.blackjackcompose

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.programmersbox.blackjackcompose.ui.theme.BlackjackComposeTheme
import com.programmersbox.funutils.cards.Deck

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
        //Greeting("Android")J

        val deck = Deck.defaultDeck()

        LazyVerticalGrid(
            cells = GridCells.Adaptive(100.dp)
        ) {
            items(deck.draw(14)) {
                PlayingCard(card = it, modifier = Modifier.padding(5.dp))
            }
        }

    }
}