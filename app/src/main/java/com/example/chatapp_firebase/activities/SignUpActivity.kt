package com.example.chatapp_firebase.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatapp_firebase.R
import com.example.chatapp_firebase.databinding.ActivitySignUpBinding
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var encodedImage : String
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        setListener()

    }

    private fun setListener(){
        binding.textSignIn.setOnClickListener {
           finish()
        }
        binding.buttonSignUp.setOnClickListener {
            if(isValidSignUpDetails()){
                signUp()
            }
        }
        binding.layoutImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            pickImage.launch(intent)
        }
        binding.togglePasswordVisibility.setOnClickListener{
            togglePasswordVisibility(binding.inputPassword, binding.togglePasswordVisibility)
        }
        binding.toggleConfirmPasswordVisibility.setOnClickListener{
            togglePasswordVisibility(binding.inputConfirmPassword, binding.toggleConfirmPasswordVisibility)
        }

    }
    private fun showToast(message: String){
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
    private fun signUp() {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            Constants.KEY_NAME to binding.inputName.text.toString(),
            Constants.KEY_EMAIL to binding.inputEmail.text.toString(),
            Constants.KEY_PASSWORD to binding.inputPassword.text.toString(),
            Constants.KEY_IMAGE to encodedImage
        )
        database.collection(Constants.KEY_COLLECTION_USERS)
            .add(user)
            .addOnSuccessListener { documentReference ->
                loading(false)
                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                preferenceManager.putString(Constants.KEY_USER_ID, documentReference.id)
                preferenceManager.putString(Constants.KEY_NAME, binding.inputName.text.toString())
                preferenceManager.putString(Constants.KEY_IMAGE, encodedImage)
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
            }
            .addOnFailureListener { exception ->
                loading(false)
                showToast(exception.message.toString())
            }
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



    private fun isValidSignUpDetails(): Boolean {
        if(encodedImage.isEmpty()){
            showToast("Please select profile image")
            return false
        }else if(binding.inputName.text.toString().trim().isEmpty()) {
            showToast("Please enter name")
            return false
        }else if(binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Please enter email")
            return false
        }else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast("Please enter valid email")
            return false
        }else if(binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Please enter password")
            return false
        }else if(binding.inputConfirmPassword.text.toString().trim().isEmpty()) {
            showToast("Confirm your password ")
            return false
        }else if(binding.inputPassword.text.toString() != binding.inputConfirmPassword.text.toString()) {
            showToast("Password and confirm password should be same")
            return false
        }else{
            return true
        }
    }

    private fun loading(isLoading: Boolean){
        if(isLoading){
            binding.buttonSignUp.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        }else{
            binding.progressBar.visibility = View.INVISIBLE
            binding.buttonSignUp.visibility = View.VISIBLE
        }
    }
    private fun togglePasswordVisibility(editText: EditText, imageView: ImageView) {
        if (editText.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            imageView.setImageResource(R.drawable.baseline_visibility_off_24)
        } else {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imageView.setImageResource(R.drawable.baseline_visibility_24)
        }
        editText.setSelection(editText.text.length)
    }
}