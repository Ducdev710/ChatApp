package com.example.chatapp_firebase.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.chatapp_firebase.R
import com.example.chatapp_firebase.databinding.ActivitySplashScreenBinding
import com.example.chatapp_firebase.databinding.ActivityUserProfileBinding
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(applicationContext)

        val blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.blink)
        binding.textView1.startAnimation(blinkAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, SignInActivity::class.java))
            }
            finish()
        }, 2000) //2s delay
    }
}