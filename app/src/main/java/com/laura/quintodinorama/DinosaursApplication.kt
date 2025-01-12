package com.laura.quintodinorama

import android.app.Application
import com.google.firebase.auth.FirebaseUser

class DinosaursApplication: Application() {
    companion object {

        // Constants
        const val PATH_DINOSAURS = "dinorama"
        const val CURIOSITIES = "curiosity"
        const val wikipediaDefaulUrl = "https://es.wikipedia.org/wiki/"

        // Map of colors associated with suborders
        val suborderColor = mapOf(
            "Sauropodomorpha" to R.color.sauropodomorpha,
            "Theropoda" to R.color.theropoda,
            "Guaibasauridae" to R.color.guaibasauridae,
            "Neornithischia" to R.color.neornithischia,
            "Thyreophora" to R.color.thyreophora,
            "eraDefault" to R.color.era_default,
        )

        // Current user
        var currentUser: FirebaseUser? = null
    }
}