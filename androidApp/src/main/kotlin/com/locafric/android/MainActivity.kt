package com.locafric.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.locafric.android.ui.EcranAccueilLocataire
import com.locafric.android.ui.InfosLocation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Données d'exemple — viendront du backend une fois connecté
                    EcranAccueilLocataire(
                        infos = InfosLocation(
                            nomBien = "Villa Ngor, chambre salon",
                            ville = "Dakar",
                            quartier = "Ngor",
                            nombrePersonnes = 2,
                            montantLoyer = "75 000 F",
                            dateProchainPaiement = "5 sept."
                        ),
                        onVoirContrat = { },
                        onSignalerReparation = { },
                        onContacterBailleur = { }
                    )
                }
            }
        }
    }
}