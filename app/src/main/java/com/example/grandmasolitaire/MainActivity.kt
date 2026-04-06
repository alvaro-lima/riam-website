package com.example.grandmasolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B6C3B)) {
                    SolitaireScreen()
                }
            }
        }
    }
}

enum class Suit(val symbol: String, val color: Color) {
    HEARTS("♥", Color(0xFFD32F2F)),
    DIAMONDS("♦", Color(0xFFD32F2F)),
    CLUBS("♣", Color(0xFF212121)),
    SPADES("♠", Color(0xFF212121));
}

data class Card(val rank: Int, val suit: Suit)

data class TableauPile(val cards: MutableList<Card>)

private data class GameState(
    val stock: MutableList<Card>,
    val waste: MutableList<Card>,
    val foundations: MutableMap<Suit, MutableList<Card>>,
    val tableau: MutableList<TableauPile>,
    val message: String,
)

private fun createDeck(): MutableList<Card> {
    val cards = mutableListOf<Card>()
    for (suit in Suit.entries) {
        for (rank in 1..13) {
            cards.add(Card(rank, suit))
        }
    }
    return cards
}

private fun shuffledDeck(): MutableList<Card> =
    createDeck().shuffled(Random(System.currentTimeMillis())).toMutableList()

private fun dealGame(): GameState {
    val deck = shuffledDeck()
    val tableau = MutableList(7) { TableauPile(mutableListOf()) }

    for (col in 0..6) {
        repeat(col + 1) {
            tableau[col].cards.add(deck.removeAt(deck.lastIndex))
        }
    }

    return GameState(
        stock = deck,
        waste = mutableListOf(),
        foundations = Suit.entries.associateWith { mutableListOf<Card>() }.toMutableMap(),
        tableau = tableau,
        message = "Tap deck to draw, then tap a pile to move.",
    )
}

private fun cardLabel(card: Card): String {
    val rank = when (card.rank) {
        1 -> "A"
        11 -> "J"
        12 -> "Q"
        13 -> "K"
        else -> card.rank.toString()
    }
    return "$rank${card.suit.symbol}"
}

private fun canPlaceOnTableau(card: Card, top: Card?): Boolean {
    if (top == null) return card.rank == 13
    val oppositeColor = top.suit.color != card.suit.color
    return oppositeColor && card.rank == top.rank - 1
}

private fun canPlaceOnFoundation(card: Card, foundationTop: Card?): Boolean {
    if (foundationTop == null) return card.rank == 1
    return card.suit == foundationTop.suit && card.rank == foundationTop.rank + 1
}

@Composable
private fun SolitaireScreen() {
    var game by remember { mutableStateOf(dealGame()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Grandma Solitaire (No Ads)",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = game.message,
            color = Color.White,
            fontSize = 20.sp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BigPile(
                title = "Deck (${game.stock.size})",
                value = if (game.stock.isEmpty()) "↺" else "Draw",
                onTap = {
                    val stock = game.stock.toMutableList()
                    val waste = game.waste.toMutableList()

                    if (stock.isEmpty()) {
                        val recycled = waste.reversed().toMutableList()
                        game = game.copy(stock = recycled, waste = mutableListOf(), message = "Deck reset")
                    } else {
                        waste.add(stock.removeAt(stock.lastIndex))
                        game = game.copy(stock = stock, waste = waste, message = "Card drawn")
                    }
                },
            )

            BigPile(
                title = "Waste",
                value = game.waste.lastOrNull()?.let(::cardLabel) ?: "Empty",
                valueColor = game.waste.lastOrNull()?.suit?.color ?: Color.White,
            )

            Button(onClick = { game = dealGame() }) {
                Text("New Game", fontSize = 20.sp)
            }
        }

        Text(text = "Foundations", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Suit.entries.forEach { suit ->
                val foundation = game.foundations[suit].orEmpty()
                BigPile(
                    title = suit.symbol,
                    value = foundation.lastOrNull()?.let(::cardLabel) ?: "—",
                    valueColor = suit.color,
                    onTap = {
                        val moving = game.waste.lastOrNull() ?: return@BigPile
                        val top = game.foundations[suit].orEmpty().lastOrNull()
                        if (canPlaceOnFoundation(moving, top)) {
                            val newWaste = game.waste.toMutableList().apply { removeAt(lastIndex) }
                            val newFoundations = game.foundations.toMutableMap()
                            val updated = newFoundations[suit].orEmpty().toMutableList()
                            updated.add(moving)
                            newFoundations[suit] = updated
                            game = game.copy(
                                waste = newWaste,
                                foundations = newFoundations,
                                message = "Moved ${cardLabel(moving)} to foundation",
                            )
                        } else {
                            game = game.copy(message = "That card can't go there")
                        }
                    },
                )
            }
        }

        Text(text = "Tableau", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    game.tableau.forEachIndexed { index, pile ->
                        BigPile(
                            title = "P${index + 1}",
                            value = pile.cards.lastOrNull()?.let(::cardLabel) ?: "Empty",
                            valueColor = pile.cards.lastOrNull()?.suit?.color ?: Color.White,
                            onTap = {
                                val moving = game.waste.lastOrNull() ?: return@BigPile
                                val top = pile.cards.lastOrNull()
                                if (canPlaceOnTableau(moving, top)) {
                                    val newWaste = game.waste.toMutableList().apply { removeAt(lastIndex) }
                                    val newTableau = game.tableau.toMutableList()
                                    val destination = newTableau[index].cards.toMutableList()
                                    destination.add(moving)
                                    newTableau[index] = TableauPile(destination)
                                    game = game.copy(
                                        waste = newWaste,
                                        tableau = newTableau,
                                        message = "Moved ${cardLabel(moving)} to pile ${index + 1}",
                                    )
                                } else {
                                    game = game.copy(message = "Place lower rank with opposite color")
                                }
                            },
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tip: This version keeps cards large and simple for easy reading.",
                    color = Color.White,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun BigPile(
    title: String,
    value: String,
    valueColor: Color = Color.White,
    onTap: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = Color.White, fontSize = 18.sp)
        Box(
            modifier = Modifier
                .size(width = 108.dp, height = 148.dp)
                .border(2.dp, Color.White, shape)
                .background(Color(0xFF1B5E20), shape)
                .let {
                    if (onTap != null) {
                        it.clickable { onTap() }
                    } else {
                        it
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                color = valueColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
