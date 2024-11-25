package com.example.chatapp_firebase.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatapp_firebase.R
import com.example.chatapp_firebase.databinding.ActivitySignInBinding
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager = PreferenceManager(applicationContext)

        // Check if user is already signed in
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListener()
    }
    private fun setListener(){
        binding.textCreateNewAccount.setOnClickListener {
            startActivity(Intent(applicationContext, SignUpActivity::class.java))
        }
        binding.buttonSignIn.setOnClickListener {
            if(isValidSignInDetails()){
                signIn()
            }
        }
        binding.togglePasswordVisibility.setOnClickListener{
            togglePasswordVisibility(binding.inputPassword, binding.togglePasswordVisibility)
        }

    }
    private fun signIn(){
        loading(true)
        val database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.text.toString())
            .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.text.toString())
            .get()
            .addOnCompleteListener { task ->
                if(task.isSuccessful && task.result != null && task.result.documents.size > 0){
                    val documentSnapshot = task.result.documents[0]
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true)
                    preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.id)
                    preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME)!!)
                    preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE)!!)
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }else{
                    loading(false)
                    showToast("Unable to sign in")
                }
            }
    }

    private fun loading(isLoading: Boolean){
        if(isLoading){
            binding.buttonSignIn.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        }else{
            binding.buttonSignIn.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    private fun showToast(message: String){
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
    private fun isValidSignInDetails(): Boolean {
        if (binding.inputEmail.text.toString().trim().isEmpty()) {
            showToast("Enter email")
            return false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.text.toString()).matches()) {
            showToast("Enter valid email")
            return false
        } else if (binding.inputPassword.text.toString().trim().isEmpty()) {
            showToast("Enter password")
            return false
        }else{
            return true
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