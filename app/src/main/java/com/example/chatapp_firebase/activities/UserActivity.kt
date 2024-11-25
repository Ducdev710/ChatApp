package com.example.chatapp_firebase.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import com.example.chatapp_firebase.adapters.UsersAdapter
import com.example.chatapp_firebase.databinding.ActivityUserBinding
import com.example.chatapp_firebase.listeners.UserListener
import com.example.chatapp_firebase.models.User
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager
import com.google.firebase.firestore.FirebaseFirestore

class UserActivity : BaseActivity() , UserListener {

    private lateinit var binding: ActivityUserBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var usersAdapter: UsersAdapter
    private lateinit var users: MutableList<User>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        users = mutableListOf()
        usersAdapter = UsersAdapter(users, this)
        binding.usersRecyclerView.adapter = usersAdapter
        getUsers()
        setListeners()
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            finish()
        }
        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                getUsers(newText)
                return true
            }
        })
    }

    private fun getUsers(query: String? = null) {
        loading(true)
        val database = FirebaseFirestore.getInstance()
        database.collection(Constants.KEY_COLLECTION_USERS)
            .get()
            .addOnCompleteListener { task ->
                loading(false)
                val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)
                if (task.isSuccessful && task.result != null) {
                    val users = mutableListOf<User>()
                    for (queryDocumentSnapshot in task.result!!) {
                        if (currentUserId == queryDocumentSnapshot.id) {
                            continue
                        }
                        val user = User(
                            name = queryDocumentSnapshot.getString(Constants.KEY_NAME).toString(),
                            email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL).toString(),
                            image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE).toString(),
                            token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN).toString(),
                            id = queryDocumentSnapshot.id,
                            password = ""
                        )
                        if (query == null || user.name.contains(query, true)) {
                            users.add(user)
                        }
                    }
                    if (users.isNotEmpty()) {
                        usersAdapter = UsersAdapter(users, this)
                        binding.usersRecyclerView.adapter = usersAdapter
                        binding.usersRecyclerView.visibility = View.VISIBLE
                        binding.textErrorMessage.visibility = View.GONE
                    }else{
                        showErrorMessage()
                    }
                }else{
                    showErrorMessage()
                }
            }


    }

    private fun showErrorMessage() {
        binding.textErrorMessage.text = String.format("%s","No users available")
        binding.textErrorMessage.visibility = View.VISIBLE
        binding.usersRecyclerView.visibility = View.GONE
    }

    private fun loading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
        }
    }

    override fun onUserClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
        finish()
    }
}