package com.locafric.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

data class NouveauBien(
    val titre: String,
    val localisation: Localisation,
    val type: String,
    val capacite: Int,
    val loyer: String,
    val description: String,
    val photos: List<Uri>
)

@Composable
fun EcranAjouterBien(
    onValider: (NouveauBien) -> Unit,
    onAnnuler: () -> Unit
) {
    var titre by remember { mutableStateOf("") }
    var localisation by remember { mutableStateOf(Localisation("", "", "")) }
    var type by remember { mutableStateOf("") }
    var capacite by remember { mutableStateOf("") }
    var loyer by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val selecteurPhotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> photos = photos + uris }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondClair)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Ajouter un bien", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Photos du bien", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photos) { uri ->
                Box(modifier = Modifier.size(84.dp)) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                    )
                    IconButton(
                        onClick = { photos = photos - uri },
                        modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Retirer", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFDDE3EA)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { selecteurPhotos.launch("image/*") }) {
                        Icon(Icons.Default.Add, contentDescription = "Ajouter une photo", tint = BleuPrincipal)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = titre,
            onValueChange = { titre = it },
            label = { Text("Titre du bien") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        SelecteurLocalisation(
            localisation = localisation,
            onLocalisationChange = { localisation = it }
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text("Type (chambre simple, chambre salon, appartement...)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = capacite,
                onValueChange = { capacite = it },
                label = { Text("Capacité (personnes)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = loyer,
                onValueChange = { loyer = it },
                label = { Text("Loyer") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) {
                Text("Annuler")
            }
            Button(
                onClick = {
                    onValider(
                        NouveauBien(titre, localisation, type, capacite.toIntOrNull() ?: 1, loyer, description, photos)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
                modifier = Modifier.weight(1f)
            ) {
                Text("Publier")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}