package com.laura.quintodinorama.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.ActivityDinosaurDetailsBinding
import com.laura.quintodinorama.retrofit.entities.Dinosaur

class DinosaurDetailsActivity : AppCompatActivity() {
    // Variable initialization
    private lateinit var mBinding: ActivityDinosaurDetailsBinding
    private lateinit var mDinoramaRef: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mBinding = ActivityDinosaurDetailsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Initialize Firebase reference
        mDinoramaRef = FirebaseDatabase.getInstance().reference

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get dinosaur ID from the Intent
        val dinosaurId = intent.getStringExtra("DINOSAUR_ID")

        // Verify that the dinosaur ID is not null
        if (dinosaurId != null) {
            loadDinosaurDetails(dinosaurId) // Load dinosaur details
        } else {
            Toast.makeText(this, "No se ha podido cargar la información del dinosaurio", Toast.LENGTH_SHORT).show()
            finish() // If the ID is null, finish the activity
        }
    }

    // Load dinosaur details from Firebase
    private fun loadDinosaurDetails(dinosaurId: String) {
        val dinosaurRef  = mDinoramaRef.child("dinorama").child(dinosaurId)

        dinosaurRef.get().addOnSuccessListener { snapshot ->
            // If the dinosaur exists, update the UI
            if (snapshot.exists()) {
                val dinosaur = snapshot.getValue(Dinosaur::class.java)
                dinosaur?.let {
                    updateUI(it)
                } ?: run {
                    Toast.makeText(this, "No se pudieron obtener los datos del dinosaurio", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "El dinosaurio no existe en la base de datos", Toast.LENGTH_SHORT).show()
            }
        } .addOnFailureListener { error ->
            Toast.makeText(this, "Error al cargar los datos: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Update the UI with the dinosaur data
    private fun updateUI(dinosaur: Dinosaur) {
        with(mBinding){
            tvTitleDetails.text = dinosaur.name ?: "Nombre desconocido"
            tvDescription.text = dinosaur.description ?: "Descripción no disponible"

            // Process the era (only the first word)
            val eraFormatted = dinosaur.era.split(",").firstOrNull()
            tvEra.text = eraFormatted ?: "Era desconocida"

            tvOrden.text = dinosaur.order ?: "Orden desconocido"
            tvSuborden.text = dinosaur.suborder ?: "Suborden desconocido"
            tvDieta.text = dinosaur.feeding ?: "Dieta desconocido"
            tvTamanyo.text = dinosaur.size ?: "Tamaño desconocido"
            tvPeso.text = dinosaur.weight ?: "Peso desconocido"


            // Button text depending on the dinosaur
            btnOrdenDetail.text = dinosaur.order ?: "Orden"
            btnEraDetail.text = eraFormatted ?: "Era"

            // Link Wikipedia to the buttons
            btnOrdenDetail.setOnClickListener {
                val order = dinosaur.order
                if ( order != null) {
                    wikipediaOrder(order)
                } else {
                    val urlDefault = "${DinosaursApplication.wikipediaDefaulUrl}${"Orden (biología)".replace(" ", "_")}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDefault))
                    startActivity(intent)
                }
            }

            btnEraDetail.setOnClickListener {
                val era = eraFormatted
                if ( era != null) {
                    wikipediaEra(era)
                } else {
                    val urlDefault = "${DinosaursApplication.wikipediaDefaulUrl}${"Era mesozoica".replace(" ", "_")}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlDefault))
                    startActivity(intent)
                }
            }

            Glide.with(this@DinosaurDetailsActivity)
                .load(dinosaur.detail_image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(imgDetailDinosaur)
        }
    }

    // Functions to open Wikipedia
    private fun wikipediaOrder(order: String) {
        val urlOrden = "${DinosaursApplication.wikipediaDefaulUrl}${order.replace(" ", "_")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlOrden))
        startActivity(intent)
    }
    private fun wikipediaEra(era: String) {
        val urlEra = "${DinosaursApplication.wikipediaDefaulUrl}${era.replace(" ", "_")}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlEra))
        startActivity(intent)
    }
}