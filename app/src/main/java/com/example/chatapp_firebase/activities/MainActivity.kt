package com.example.chatapp_firebase.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import androidx.appcompat.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp_firebase.adapters.RecentConversationsAdapter
import com.example.chatapp_firebase.databinding.ActivityMainBinding
import com.example.chatapp_firebase.listeners.ConversionListener
import com.example.chatapp_firebase.models.ChatMessage
import com.example.chatapp_firebase.models.User
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.messaging.FirebaseMessaging
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.ContextCompat

class MainActivity : BaseActivity(), ConversionListener{

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var conversations: MutableList<ChatMessage>
    private lateinit var conversationsAdapter: RecentConversationsAdapter
    private lateinit var database: FirebaseFirestore
    private lateinit var swipeBackground: ColorDrawable
    private lateinit var deleteIcon: Drawable

    companion object {
        const val REQUEST_CODE_UPDATE_PROFILE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        preferenceManager = PreferenceManager(applicationContext)
        database = FirebaseFirestore.getInstance()
        loadUserDetails()
        getToken()
        setListeners()
        init()
        listenConversations()
        /*setupSwipeToDelete()*/
    }

    private fun init() {
        conversations = ArrayList()
        val currentUserId = preferenceManager.getString(Constants.KEY_USER_ID)!!
        conversationsAdapter = RecentConversationsAdapter(conversations, this, currentUserId)
        binding.conversationsRecyclerView.adapter = conversationsAdapter
        database = FirebaseFirestore.getInstance()
        swipeBackground = ColorDrawable(Color.RED)
        deleteIcon = ContextCompat.getDrawable(applicationContext, android.R.drawable.ic_menu_delete)!!
    }

    /*private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val chatMessage = conversations[position]
                showDeleteConfirmationDialog(chatMessage, position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                if (dX < 0) {
                    swipeBackground.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteIcon.setBounds(
                        itemView.right - iconMargin - deleteIcon.intrinsicWidth,
                        itemView.top + iconMargin,
                        itemView.right - iconMargin,
                        itemView.bottom - iconMargin
                    )
                }
                swipeBackground.draw(c)
                c.save()
                if (dX < 0) {
                    c.clipRect(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                }
                deleteIcon.draw(c)
                c.restore()
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.conversationsRecyclerView)
    }

    private fun showDeleteConfirmationDialog(chatMessage: ChatMessage, position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Are you sure you want to delete this conversation?")
            .setPositiveButton("Delete") { dialog, id ->
                deleteConversation(chatMessage, position)
            }
            .setNegativeButton("Cancel") { dialog, id ->
                conversationsAdapter.notifyItemChanged(position)
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
    }

    private fun deleteConversation(chatMessage: ChatMessage, position: Int) {
        val conversationId = chatMessage.conversionId
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .delete()
            .addOnSuccessListener {
                showToast("Conversation deleted")
                conversations.removeAt(position)
                conversationsAdapter.notifyItemRemoved(position)
            }
            .addOnFailureListener { e ->
                showToast("Failed to delete conversation: ${e.message}")
                conversationsAdapter.notifyItemChanged(position)
            }
    }*/

    private fun setListeners() {
        binding.imageSignOut.setOnClickListener {
            signOut()
        }
        binding.fabNewChat.setOnClickListener {
            startActivity(Intent(applicationContext, UserActivity::class.java))
        }
        binding.searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterConversations(newText)
                return true
            }
        })
        binding.imageProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_UPDATE_PROFILE)
        }
    }
    private fun loadUserDetails() {
        binding.textName.text = preferenceManager.getString(Constants.KEY_NAME)
        val bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imageProfile.setImageBitmap(bitmap)
    }
    private fun showToast(message: String){
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    private fun listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
            .addSnapshotListener(eventListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private val eventListener = EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }

        if (value != null) {
            for (documentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                    val receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                    val chatMessage = ChatMessage().apply {
                        this.senderId = senderId!!
                        this.receiverId = receiverId!!
                        /*this.senderName = if (senderId == preferenceManager.getString(Constants.KEY_USER_ID)) {
                            "You"
                        } else {
                            documentChange.document.getString(Constants.KEY_SENDER_NAME).toString()
                        }*/
                    }
                    if (preferenceManager.getString(Constants.KEY_USER_ID) == chatMessage.senderId) {
                        chatMessage.conversionImage = documentChange.document.getString(Constants.KEY_RECEIVER_IMAGE).toString()
                        chatMessage.conversionName = documentChange.document.getString(Constants.KEY_RECEIVER_NAME).toString()
                        chatMessage.conversionId = documentChange.document.getString(Constants.KEY_RECEIVER_ID).toString()
                    } else {
                        chatMessage.conversionImage = documentChange.document.getString(Constants.KEY_SENDER_IMAGE).toString()
                        chatMessage.conversionName = documentChange.document.getString(Constants.KEY_SENDER_NAME).toString()
                        chatMessage.conversionId = documentChange.document.getString(Constants.KEY_SENDER_ID).toString()
                    }
                    chatMessage.message = documentChange.document.getString(Constants.KEY_LAST_MESSAGE).toString()
                    chatMessage.dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                    conversations.add(chatMessage)
                } else if (documentChange.type == DocumentChange.Type.MODIFIED) {
                    for (i in conversations.indices) {
                        val senderId = documentChange.document.getString(Constants.KEY_SENDER_ID)
                        val receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID)
                        if (conversations[i].senderId == senderId && conversations[i].receiverId == receiverId) {
                            conversations[i].message = documentChange.document.getString(Constants.KEY_LAST_MESSAGE)!!
                            conversations[i].dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                            break
                        }
                    }
                }
            }
            conversations.sortWith { obj1, obj2 -> obj2.dateObject.compareTo(obj1.dateObject) }
            conversationsAdapter.notifyDataSetChanged()
            binding.conversationsRecyclerView.smoothScrollToPosition(0)
            binding.conversationsRecyclerView.visibility = android.view.View.VISIBLE
            binding.progressBar.visibility = android.view.View.GONE
        }
    }

    private fun filterConversations(query: String?) {
        if (query.isNullOrEmpty() ) {
            conversationsAdapter.updateList(conversations)
        } else {
            val filteredConversations = conversations.filter {
                it.conversionName.contains(query, true) || it.message.contains(query, true)
            }
            conversationsAdapter.updateList(filteredConversations)
        }
    }

    private fun getToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            updateToken(token)
        }
    }

    private fun updateToken(token: String) {
        val database = FirebaseFirestore.getInstance()
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
            .addOnFailureListener { e ->
                showToast("Unable to update token: ${e.message}")
            }
    }
    private fun signOut() {
        showToast("Signing out...")
        val database = FirebaseFirestore.getInstance()
        val documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
            .document(preferenceManager.getString(Constants.KEY_USER_ID)!!)
        val updates = hashMapOf<String, Any>(
            Constants.KEY_FCM_TOKEN to FieldValue.delete()
        )
        documentReference.update(updates)
            .addOnSuccessListener {
                preferenceManager.clear()
                startActivity(Intent(applicationContext, SignInActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                showToast("Unable to sign out")
            }
    }

    override fun onConversionClicked(user: User) {
        val intent = Intent(applicationContext, ChatActivity::class.java)
        intent.putExtra(Constants.KEY_USER, user)
        startActivity(intent)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE_PROFILE && resultCode == RESULT_OK) {
            // Update the UI with the new user details
            loadUserDetails()
        }
    }
}