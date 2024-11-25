package com.example.chatapp_firebase.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chatapp_firebase.R
import com.example.chatapp_firebase.databinding.ActivityUserProfileBinding
import com.example.chatapp_firebase.models.User
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var isEditable = false
    private var encodedImage: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(applicationContext)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        getUserDetails()
        binding.imageBack.setOnClickListener {
            finish()
        }
        binding.textChangeInformation.setOnClickListener {
            toggleEditing()
        }
        binding.buttonSave.isEnabled = false
        binding.buttonSave.alpha = 0.5f
        binding.buttonSave.setOnClickListener {
            if (isEditable) {
                val name = binding.inputName.text.toString()
                val email = binding.inputEmail.text.toString()
                val password = binding.inputPassword.text.toString()
                val confirmPassword = binding.inputConfirmPassword.text.toString()
                if (name.isEmpty()) {
                    binding.inputName.error = "Name cannot be empty"
                } else if (email.isEmpty()) {
                    binding.inputEmail.error = "Email cannot be empty"
                } else if (password.isEmpty()) {
                    binding.inputPassword.error = "Password cannot be empty"
                } else if (confirmPassword.isEmpty()) {
                    binding.inputConfirmPassword.error = "Confirm password cannot be empty"
                } else if (password != confirmPassword) {
                    binding.inputConfirmPassword.error = "Password and confirm password must be same"
                } else {
                    val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID).toString()
                    firestore.collection(Constants.KEY_COLLECTION_USERS)
                        .document(currentUserId)
                        .get()
                        .addOnSuccessListener { document ->
                            val existingImage = document.getString(Constants.KEY_IMAGE).toString()
                            val user = User(
                                id = currentUserId,
                                name = name,
                                email = email,
                                image = if (encodedImage.isEmpty()) existingImage else encodedImage,
                                token = preferenceManager.getString(Constants.KEY_FCM_TOKEN).toString(),
                                password = password
                            )
                            updateUser(user)
                        }
                        .addOnFailureListener {
                            Log.e("UserProfileActivity", "Error fetching user details", it)
                        }
                }
            } else {
                toggleEditing()
            }
        }
    }

    private fun getUserDetails() {
        val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)
        if (currentUserId != null) {
            firestore.collection(Constants.KEY_COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        val document = task.result
                        val user = User(
                            id = document.getString(Constants.KEY_USER_ID).toString(),
                            name = document.getString(Constants.KEY_NAME).toString(),
                            email = document.getString(Constants.KEY_EMAIL).toString(),
                            image = document.getString(Constants.KEY_IMAGE).toString(),
                            token = document.getString(Constants.KEY_FCM_TOKEN).toString(),
                            password = document.getString(Constants.KEY_PASSWORD).toString()
                        )
                        updateUI(user)
                    } else {
                        Log.e("UserProfileActivity", "Error fetching user details")
                    }
                }
                .addOnFailureListener {
                    Log.e("UserProfileActivity", "Error fetching user details", it)
                }
        } else {
            Log.e("UserProfileActivity", "Current user ID is null")
        }
    }

    private fun updateUI(user: User) {
        binding.inputName.setText(user.name)
        binding.inputEmail.setText(user.email)
        binding.inputPassword.setText(user.password)
        binding.inputConfirmPassword.setText(user.password)

        // Make EditText fields non-editable and set text as hint
        setEditTextNonEditable(binding.inputName)
        setEditTextNonEditable(binding.inputEmail)
        setEditTextNonEditable(binding.inputPassword)
        setEditTextNonEditable(binding.inputConfirmPassword)

        if (user.image.isNotEmpty()) {
            val imageBytes = Base64.decode(user.image, Base64.DEFAULT)
            if (imageBytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.imageProfile.setImageBitmap(bitmap)
                binding.textAddImage.visibility = View.GONE
            } else {
                binding.imageProfile.setImageResource(R.drawable.baseline_visibility_24)
            }
        } else {
            binding.imageProfile.setImageResource(R.drawable.baseline_visibility_off_24)
        }
    }

    private fun toggleEditing() {
        if (isEditable) {
            setEditTextNonEditable(binding.inputName)
            setEditTextNonEditable(binding.inputEmail)
            setEditTextNonEditable(binding.inputPassword)
            setEditTextNonEditable(binding.inputConfirmPassword)
            binding.buttonSave.isEnabled = false
            binding.buttonSave.alpha = 0.5f
            binding.imageProfile.setOnClickListener(null)
        } else {
            setEditTextEditable(binding.inputName)
            setEditTextEditable(binding.inputEmail)
            setEditTextEditable(binding.inputPassword)
            setEditTextEditable(binding.inputConfirmPassword)
            binding.buttonSave.isEnabled = true
            binding.buttonSave.alpha = 1.0f
            binding.imageProfile.setOnClickListener{
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                pickImage.launch(intent)
            }
        }
        isEditable = !isEditable
    }

    private fun setEditTextNonEditable(editText: EditText) {
        editText.hint = editText.text
        editText.setText("")
        editText.inputType = InputType.TYPE_NULL
        editText.isFocusable = false
        editText.setTextColor(ContextCompat.getColor(this, R.color.primary_text_faded))
    }

    private fun setEditTextEditable(editText: EditText) {
        editText.setText(editText.hint)
        editText.hint = ""
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.isFocusable = true
        editText.isFocusableInTouchMode = true
        editText.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val previewWidth = 150
        val previewHeight = bitmap.height * previewWidth / bitmap.width
        val previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false)
        val byteArrayOutputStream = ByteArrayOutputStream()
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data != null) {
                val imageUri: Uri? = result.data?.data
                try {
                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    binding.imageProfile.setImageBitmap(bitmap)
                    binding.textAddImage.visibility = View.GONE
                    encodedImage = encodeImage(bitmap)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun updateUser(user: User) {
        firestore.collection(Constants.KEY_COLLECTION_USERS)
            .document(user.id)
            .set(user)
            .addOnSuccessListener {
                preferenceManager.putString(Constants.KEY_NAME, user.name)
                preferenceManager.putString(Constants.KEY_EMAIL, user.email)
                preferenceManager.putString(Constants.KEY_IMAGE, user.image)
                preferenceManager.putString(Constants.KEY_FCM_TOKEN, user.token)
                preferenceManager.putString(Constants.KEY_PASSWORD, user.password)
                updateConversations(user)
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                Log.e("UserProfileActivity", "Error updating user details", it)
            }
    }
    private fun updateConversations(user: User) {
        val senderUpdates = hashMapOf<String, Any>(
            Constants.KEY_SENDER_NAME to user.name,
            Constants.KEY_SENDER_IMAGE to user.image
        )

        val receiverUpdates = hashMapOf<String, Any>(
            Constants.KEY_RECEIVER_NAME to user.name,
            Constants.KEY_RECEIVER_IMAGE to user.image
        )

        firestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, user.id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update(senderUpdates)
                }
            }

        firestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, user.id)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update(receiverUpdates)
                }
            }
    }
}