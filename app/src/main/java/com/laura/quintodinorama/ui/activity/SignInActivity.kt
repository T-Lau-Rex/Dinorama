package com.laura.quintodinorama.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {

    // Variable initialization
    private lateinit var binding: ActivitySignInBinding
    private lateinit var mFirebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set the background for the sign-in screen
        setUpBackground()

        // Firebase instance
        mFirebaseAuth = FirebaseAuth.getInstance()

        // Link to go to the sign-up screen
        setUpLoginButton()

        // Link to go to the sign-in screen
        setUpSignInButton()

        // Functions to validate fields
        setupEmailWatcher()
        setupPasswordWatcher()
    }

    // Function to set up the background of the screen
    private fun setUpBackground() {
        val backgroundSignin: ImageView = binding.root.findViewById(R.id.myBackground)
        Glide.with(this)
            .load(R.drawable.fondo_login)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(backgroundSignin)
    }

    // Set up the sign-up button
    private fun setUpLoginButton() {
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Set up the sign-in button
    private fun setUpSignInButton() {
        binding.signInBtn.setOnClickListener {
            signInUser()
        }
    }

    // Sign-in process
    private fun signInUser() {
        val email = binding.tielSignInEmail.text.toString().trim()
        val password = binding.tielSignInPassword.text.toString().trim()

        // Field validation
        if (email.isEmpty()) {
            binding.tilSignInEmail.error = "Ingresa tu email"
            return
        }
        if (password.isEmpty()){
            binding.tilSingInPassword.error = "Escribe tu contraseña"
            return
        }

        // Firebase sign-in
        mFirebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) {task ->
            if (task.isSuccessful){

                val user = mFirebaseAuth.currentUser
                if (user != null && user.isEmailVerified){
                    // Assign the user to DinosaursApplication
                    DinosaursApplication.currentUser = user

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Por favor verifica tu correo electrónico antes de continuar", Toast.LENGTH_LONG).show()
                    mFirebaseAuth.signOut()
                }
            } else {
                Toast.makeText(this, "Error en las credenciales: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Email validation
    private fun setupEmailWatcher() {
        binding.tielSignInEmail.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                when {
                    email.isEmpty() -> binding.tilSignInEmail.error = "Campo requerido"
                    else -> binding.tilSignInEmail.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    // Password validation
    private fun setupPasswordWatcher() {
        binding.tielSignInPassword.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                when {
                    password.isEmpty() -> binding.tilSingInPassword.error = "Campo requerido"
                    //password.length < 6 ->binding.tilSingInPassword.error = "La contraseña debe tener al menos 6 characteres"
                    else -> binding.tilSingInPassword.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }
}