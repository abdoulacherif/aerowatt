package com.locafric.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Message(val texte: String, val envoyeParMoi: Boolean)
data class Conversation(val id: String, val nom: String, val dernierMessage: String)

@Composable
fun EcranMessages(
    conversations: List<Conversation>,
    conversationOuverte: Conversation?,
    messages: List<Message>,
    onOuvrirConversation: (Conversation) -> Unit,
    onRetour: () -> Unit,
    onEnvoyerMessage: (String) -> Unit
) {
    Crossfade(targetState = conversationOuverte, label = "messages") { conv ->
        if (conv == null) {
            ListeConversations(conversations, onOuvrirConversation)
        } else {
            VueChat(conv, messages, onRetour, onEnvoyerMessage)
        }
    }
}

@Composable
private fun ListeConversations(
    conversations: List<Conversation>,
    onOuvrirConversation: (Conversation) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(FondClair).padding(16.dp)) {
        Text("Messages", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(conversations) { index, conv ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(index * 60L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 }
                ) {
                    Card(
                        onClick = { onOuvrirConversation(conv) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(BleuClair),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(conv.nom.take(1), color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(conv.nom, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(conv.dernierMessage, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VueChat(
    conv: Conversation,
    messages: List<Message>,
    onRetour: () -> Unit,
    onEnvoyerMessage: (String) -> Unit
) {
    var texte by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(FondClair)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BleuPrincipal).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRetour) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
            Text(conv.nom, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.envoyeParMoi) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (msg.envoyeParMoi) BleuPrincipal else Color.White)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                msg.texte,
                                color = if (msg.envoyeParMoi) Color.White else Color.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = texte,
                onValueChange = { texte = it },
                placeholder = { Text("Écrire un message...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (texte.isNotBlank()) {
                        onEnvoyerMessage(texte)
                        texte = ""
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(50)).background(BleuPrincipal)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = Color.White)
            }
        }
    }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    list: List<T>,
    content: @Composable (Int, T) -> Unit
) {
    for (i in list.indices) {
        item { content(i, list[i]) }
    }
}