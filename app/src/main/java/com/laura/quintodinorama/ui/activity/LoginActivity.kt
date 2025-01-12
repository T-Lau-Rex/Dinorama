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
import com.google.firebase.auth.userProfileChangeRequest
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    // Variable initialization
    private lateinit var binding: ActivityLoginBinding
    private lateinit var mFirebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the background for the login screen
        setUpBackground()

        // Firebase instance
        mFirebaseAuth = FirebaseAuth.getInstance()

        // Navigation to the SignIn
        binding.tvLoginNotRegistered.setOnClickListener{
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if the user is already logged in
        checkUser()

        // Login when the Login button is clicked
        binding.LoginBtn.setOnClickListener{
            loginUser()
        }

        // Set up validation for text fields
        setupNameWatcher()
        setupEmailWatcher()
        setupPasswordWatcher()

    }

    private fun setUpBackground() {
        val backgroundLogin: ImageView = binding.root.findViewById(R.id.myBackground)
        Glide.with(this)
            .load(R.drawable.fondo_login)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(backgroundLogin)    }

    //==========================================//
    //     Check if user session is started     //
    //==========================================//

    // Checks if the user already has an active session, if so, redirects the user
    // to the main screen; otherwise, stays on the login screen.
    private fun checkUser() {
        val currentUser = mFirebaseAuth.currentUser
        if (currentUser != null){
            DinosaursApplication.currentUser = currentUser
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    //==========================================//
    //         User Registration                //
    //==========================================//

    // Validation of the user's input fields, then proceeds with the registration
    // in Firebase, handling both account creation and sending a verification email.
    private fun loginUser() {
        val name = binding.tielLoginName.text.toString().trim()
        val email  = binding.tielLoginEmail.text.toString().trim()
        val password  = binding.tielLoginPassword.text.toString().trim()

        //  Validation of fields
        if (name.isEmpty()) {
            binding.tilLoginName.error = "Ingresa tu nombre de usuario"
            return
        }
        if (email.isEmpty()) {
            binding.tilLoginEmail.error = "Campo requerido"
            return
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilLoginEmail.error = "Formato de email inválido"
            return
        }
        if (password.isEmpty()) {
            binding.tilLoginPassword.error = "Campo requerido"
            return
        } else if (password.length < 6) {
            binding.tilLoginPassword.error = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        // Registration in Firebase
        mFirebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) {task ->
            if (task.isSuccessful) {
                val user = mFirebaseAuth.currentUser
                DinosaursApplication.currentUser = mFirebaseAuth.currentUser!!

                // Send verification email
                user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                    if (verificationTask.isSuccessful) {
                        Toast.makeText(this, "Se ha enviado un correo de verificación a tu correo. Por favor, verifícalo", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Error al enviar el correo de verificación", Toast.LENGTH_LONG).show()
                    }
                }

                // Update user profile name
                val profileUpdates = userProfileChangeRequest {
                    displayName = name
                }

                user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al guardar el nombre: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                Toast.makeText(this, "Error al registrar usuario: El email ya está siendo utilizada por otra cuenta", Toast.LENGTH_SHORT).show()
            }
        } //the email address is already in use by another account
    }

    //==========================================//
    //          Field Validation                //
    //==========================================//

    // Name validation
    private fun setupNameWatcher() {
        binding.tielLoginName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val name = s.toString()
                when {
                    name.isEmpty() -> binding.tilLoginName.error = "Ingresa tu nombre de usuario"
                    else -> binding.tilLoginName.error = null
                }
            }

            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    // Email validation
    private fun setupEmailWatcher() {
        binding.tielLoginEmail.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString()
                when {
                    email.isEmpty() -> binding.tilLoginEmail.error = "Campo requerido"
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.tilLoginEmail.error = "Formato de email inválido"
                    else -> binding.tilLoginEmail.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    // Password validation
    private fun setupPasswordWatcher() {
        binding.tielLoginPassword.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                when {
                    password.isEmpty() -> binding.tilLoginPassword.error = "Campo requerido"
                    password.length < 6 ->binding.tilLoginPassword.error = "La contraseña debe tener al menos 6 characteres"
                    else -> binding.tilLoginPassword.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }
}