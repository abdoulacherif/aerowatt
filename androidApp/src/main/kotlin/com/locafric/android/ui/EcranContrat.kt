package com.locafric.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EcranContrat(
    texteContrat: String,
    onContratSigne: () -> Unit
) {
    val pathSignature = remember { Path() }
    var aSigne by remember { mutableStateOf(false) }
    var version by remember { mutableStateOf(0) } // force le redessin

    Column(modifier = Modifier.fillMaxSize().background(FondClair).padding(16.dp)) {
        Text("Contrat de bail", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Text(
                texteContrat,
                fontSize = 13.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("Votre signature", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            pathSignature.moveTo(offset.x, offset.y)
                            aSigne = true
                        }
                    ) { change, _ ->
                        pathSignature.lineTo(change.position.x, change.position.y)
                        version++
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // "version" force la recomposition à chaque trait dessiné
                @Suppress("UNUSED_EXPRESSION") version
                drawPath(pathSignature, color = BleuPrincipal, style = Stroke(width = 4f))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = {
            pathSignature.reset()
            aSigne = false
            version++
        }) {
            Text("Effacer la signature", color = OrangeAccent, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onContratSigne,
            enabled = aSigne,
            colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Signer et valider le contrat")
        }
    }
}