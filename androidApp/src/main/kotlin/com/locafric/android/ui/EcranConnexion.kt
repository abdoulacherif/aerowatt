package com.locafric.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RoleUtilisateur { BAILLEUR, LOCATAIRE }

data class InfosConnexion(
    val email: String,
    val motDePasse: String,
    val role: RoleUtilisateur,
    val nomComplet: String = "" // utilisé seulement à l'inscription
)

@Composable
fun EcranConnexion(
    onConnexion: (InfosConnexion) -> Unit,
    onInscription: (InfosConnexion) -> Unit
) {
    var modeInscription by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var nomComplet by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(RoleUtilisateur.LOCATAIRE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondClair)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "LOCAFRIC",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BleuPrincipal
        )
        Text(
            text = if (modeInscription) "Créer un compte" else "Connexion",
            fontSize = 15.sp,
            color = androidx.compose.ui.graphics.Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sélecteur de rôle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButtonRole(
                label = "Locataire",
                selectionne = role == RoleUtilisateur.LOCATAIRE,
                onClick = { role = RoleUtilisateur.LOCATAIRE },
                modifier = Modifier.weight(1f)
            )
            SegmentedButtonRole(
                label = "Bailleur",
                selectionne = role == RoleUtilisateur.BAILLEUR,
                onClick = { role = RoleUtilisateur.BAILLEUR },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (modeInscription) {
            OutlinedTextField(
                value = nomComplet,
                onValueChange = { nomComplet = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = motDePasse,
            onValueChange = { motDePasse = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val infos = InfosConnexion(email, motDePasse, role, nomComplet)
                if (modeInscription) onInscription(infos) else onConnexion(infos)
            },
            colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(if (modeInscription) "S'inscrire" else "Se connecter")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { modeInscription = !modeInscription }) {
            Text(
                if (modeInscription) "J'ai déjà un compte — Se connecter"
                else "Pas de compte — S'inscrire",
                color = OrangeAccent
            )
        }
    }
}

@Composable
private fun SegmentedButtonRole(
    label: String,
    selectionne: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectionne) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = BleuPrincipal),
            modifier = modifier
        ) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}