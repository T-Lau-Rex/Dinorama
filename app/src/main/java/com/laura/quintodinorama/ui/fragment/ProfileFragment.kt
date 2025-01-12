package com.laura.quintodinorama.ui.fragment


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.firebase.ui.auth.AuthUI
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.laura.quintodinorama.DinosaursApplication
import com.laura.quintodinorama.R
import com.laura.quintodinorama.databinding.FragmentProfileBinding
import com.laura.quintodinorama.ui.activity.LoginActivity
import com.laura.quintodinorama.ui.activity.MainActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding: FragmentProfileBinding get() = _binding ?: throw IllegalStateException("Binding should not be null")

    private val userUid = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var mStorageReference: StorageReference
    private lateinit var mDinoramaRef: DatabaseReference /* TODO: Esto puede que no haga falta porque es para subirlo a Realtime Database*/

    private val RC_GALLERY = 18
    private val PATH_PROFILE_PHOTO = "profile/${userUid}/photo" // TODO: Comprobar si funciona

    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //val profileViewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        //val root: View = binding.root

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgIconPhoto.setOnClickListener { setupChangeImageCard() }
        binding.iconEditNameCorreo.setOnClickListener { setupChangeInfo() }

        mStorageReference = FirebaseStorage.getInstance().reference
        mDinoramaRef =
            FirebaseDatabase.getInstance().reference.child(PATH_PROFILE_PHOTO) // TODO: De nuevo, creo que no hace falta

        setupProfileImage()
        setupInfo()
        setupBtnLogOut()
        setupBtnDeleteAccount()
    }

    private fun setupChangeImageCard() {
        binding.clChangePhoto.visibility = View.VISIBLE
        binding.iconEditNameCorreo.visibility = View.GONE

        val imageUrl = mDinoramaRef.child("url")
        imageUrl.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(CircleCrop())
                        .into(binding.ivBigPhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        binding.btnChangePhoto.setOnClickListener { openGallery() }
        binding.btnSavePhoto.setOnClickListener { savePhotoProfile() }
        binding.btnCloseChangeImage.setOnClickListener {
            binding.clChangePhoto.visibility = View.GONE
            binding.iconEditNameCorreo.visibility = View.VISIBLE
        }
    }

    private fun setupProfileImage() {
        mDinoramaRef.child("url").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(CircleCrop())
                        .into(binding.imgIconPhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, RC_GALLERY)
    }

    private fun setupInfo() {
        binding.tvName.text = DinosaursApplication.currentUser?.displayName
        binding.tvMail.text = DinosaursApplication.currentUser?.email
    }

    private fun setupChangeInfo() {
        binding.clChangeData.visibility = View.VISIBLE

        val imageUrl = mDinoramaRef.child("url")
        imageUrl.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val imageUrl = snapshot.getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment)
                        .load(imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transform(CircleCrop())
                        .into(binding.ivSmallPhoto)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar la imagen de perfil", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        binding.tielNameChange.setText(DinosaursApplication.currentUser?.displayName)
        binding.tielMailChange.setText(DinosaursApplication.currentUser?.email)

        binding.tielPassChange.setOnClickListener {
            binding.clChangePass.visibility = View.VISIBLE
            updatePassword()
        }
        binding.btnCloseChangeData.setOnClickListener {
            binding.clChangeData.visibility = View.GONE
        }
        binding.btnSaveChanges.setOnClickListener {
            saveInfo()
        }
    }

    private fun saveInfo() {
        val newName = binding.tielNameChange.text.toString().trim()
        val newMail = binding.tielMailChange.text.toString().trim()

        // Comprueba que los campos no estén vacíos
        if (newName.isEmpty() || newMail.isEmpty()) {
            Toast.makeText(context, "Rellena todos los campo", Toast.LENGTH_SHORT).show()
            return
        }
        val user = DinosaursApplication.currentUser

        // Actualizar nombre usuario
        val profileName = userProfileChangeRequest {
            displayName = newName
        }

        user?.updateProfile(profileName)?.addOnCompleteListener { nameTask ->
            if (nameTask.isSuccessful) {
                // Actualizar correo
                user.updateEmail(newMail).addOnCompleteListener { mailTask ->
                    if (mailTask.isSuccessful) {
                        Toast.makeText(
                            context,
                            "Datos actualizados correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.clChangeData.visibility = View.GONE
                        binding.tvName.text = newName
                        binding.tvMail.text = newMail
                    } else {
                        Toast.makeText(
                            context,
                            "Error al actualizar correo: ${mailTask.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "Error al actualizar nombre: ${nameTask.exception?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updatePassword() {
        binding.clChangeData.visibility = View.GONE
        binding.btnCloseChangePass.setOnClickListener {
            binding.clChangePass.visibility = View.GONE
            binding.clChangeData.visibility = View.VISIBLE
        }
    }

    private fun setupBtnLogOut() {
        binding.btnLogOut.setOnClickListener {
            context?.let {
                val dialog = MaterialAlertDialogBuilder(it)
                    .setTitle("¿Desea cerrar la sesión actual")
                    .setPositiveButton("Cerrar") { _, _ ->
                        LogOut()
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()
            }
        }
    }

    private fun LogOut() {
        context?.let { context ->
            AuthUI.getInstance().signOut(context)
                .addOnCompleteListener {
                    Toast.makeText(context, "¡Hasta pronto!", Toast.LENGTH_SHORT).show()
                    if (_binding != null) {
                        binding.tvName.text = ""
                    }
                    val loginIntent = Intent(context, LoginActivity::class.java)
                    loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(loginIntent)
                    activity?.finish()
                }
        }
    }

    private fun setupBtnDeleteAccount() {
        binding.btnDeleteAccount.setOnClickListener {
            context?.let {
                val dialog = MaterialAlertDialogBuilder(it)
                    .setTitle("¿Desea eliminar su cuenta? Todos los datos se perderán")
                    .setPositiveButton("Eliminar") { _, _ ->
                        deleteAccount()
                    }
                    .setNegativeButton("Cancelar", null)
                    .create()
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()
            }
        }
    }

    private fun deleteAccount() {
        val user = DinosaursApplication.currentUser

        if (user?.email.isNullOrEmpty()) {
            Toast.makeText(context, "Error: No se pudo recuperar el correo del usuario.", Toast.LENGTH_SHORT).show()
            return
        }

        user?.let {
            promptForPassword { password ->
                val email = user.email!!
                val credential = EmailAuthProvider.getCredential(email, password)

                user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                    if (reauthTask.isSuccessful) {
                        user.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(context, LoginActivity::class.java))
                                    activity?.finish()
                                } else {
                                    Toast.makeText(context, "Error al eliminar cuenta ${task.exception?.message}", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        //Toast.makeText(context, "Error al reautenticar: ${reauthTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        val exception = reauthTask.exception
                        if (exception is FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(context, "Contraseña incorrecta. Por favor, inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al reautenticar: ${exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

            }
        } ?: run {
            Toast.makeText(context, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
        }

        /*user?.let {
            user.delete()
                .addOnCompleteListener {task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Cuenta eliminada correctamente", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(context, LoginActivity::class.java))
                        activity?.finish()
                    } else {
                        Toast.makeText(context, "Error al eliminar cuenta ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } ?: run {
            Toast.makeText(context, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show()
        }*/

    }

    private fun promptForPassword(callback: (String) -> Unit) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextAppearance(R.style.DialogEditText)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reautenticación requerida")
            .setMessage("Por favor, introduce tu contraseña para continuar.")
            .setView(input)
            .setPositiveButton("Aceptar") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    callback(password)
                } else {
                    Toast.makeText(context, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun savePhotoProfile() {

        if (imageUri != null) {
            Glide.with(this)
                .load(imageUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(CircleCrop())
                .into(binding.imgIconPhoto)

            postPhotoProfile()
        }

        binding.iconEditNameCorreo.visibility = View.VISIBLE
        binding.clChangePhoto.visibility = View.GONE
    }

    private fun postPhotoProfile() {
        //mStorageReference.child(PATH_PROFILE_PHOTO).child("my_photo")
        imageUri?.let { uri ->
            val storageReference = mStorageReference.child(PATH_PROFILE_PHOTO).child("my_photo")

            storageReference.putFile(uri).addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener { downloadUrl ->
                    mDinoramaRef.child("url").setValue(downloadUrl.toString())
                        .addOnSuccessListener {
                            Toast.makeText(context, "Foto de perfil guardada", Toast.LENGTH_SHORT).show()
                            Glide.with(this)
                                .load(downloadUrl.toString())
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transform(CircleCrop())
                                .into(binding.imgIconPhoto)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al guardar la URL en la base de datos", Toast.LENGTH_SHORT).show()
                        }
                }
            }
                .addOnFailureListener {
                    Toast.makeText(context, "La foto no se ha podido subir, inténtalo más tarde", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(context, "No se ha seleccionado ninguna imagen", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RC_GALLERY) {
                imageUri = data?.data
                Glide.with(this)
                    .load(imageUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transform(CircleCrop())
                    .into(binding.ivBigPhoto)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}