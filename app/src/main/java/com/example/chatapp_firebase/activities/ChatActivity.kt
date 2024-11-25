package com.example.chatapp_firebase.activities

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.opengl.ETC1.encodeImage
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.chatapp_firebase.adapters.ChatAdapter
import com.example.chatapp_firebase.databinding.ActivityChatBinding
import com.example.chatapp_firebase.models.ChatMessage
import com.example.chatapp_firebase.models.User
import com.example.chatapp_firebase.utilities.Constants
import com.example.chatapp_firebase.utilities.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var receiverUser: User
    private lateinit var chatMessages: MutableList<ChatMessage>
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var database: FirebaseFirestore
    private var conversionId: String? = null
    private var isReceiverAvailable: Boolean = false
    /*private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
            val base64String = encodeImage(bitmap)
            displaySelectedImage(base64String)
        }
    }*/


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setListeners()
        loadReceiverDetails()
        init()
        listenMessages()
    }

    private fun init(){
        preferenceManager = PreferenceManager(applicationContext)
        chatMessages = ArrayList()
        chatAdapter = ChatAdapter(chatMessages, getBitmapEncodedString(receiverUser.image), preferenceManager.getString(Constants.KEY_USER_ID)!!)
        binding.chatRecyclerView.adapter = chatAdapter
        database = FirebaseFirestore.getInstance()
    }

    private fun setListeners() {
        binding.imageBack.setOnClickListener {
            finish()
        }
        binding.layoutSend.setOnClickListener {
            sendMessage()
        }
        binding.imageInfo.setOnClickListener {
            val dialog = UserInfoDialogFragment(receiverUser, isReceiverAvailable)
            dialog.show(supportFragmentManager, "UserInfoDialog")
        }
        binding.imageAttachment.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val base64String = encodeImage(bitmap)
                displaySelectedImage(base64String)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
        Log.d("Base64Image", base64String)
        return base64String
    }

    private var base64ImageString: String? = null

    private fun displaySelectedImage(base64String: String) {
        base64ImageString = base64String
        val bytes = Base64.decode(base64String, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val drawable = BitmapDrawable(resources, bitmap)

        // Adjust the size of the drawable to fit within the EditText
        val width = binding.inputMessage.lineHeight
        val height = (drawable.intrinsicHeight * width) / drawable.intrinsicWidth
        drawable.setBounds(0, 0, width, height)

        val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
        val spannableString = SpannableString(" ")
        spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        val editableText = Editable.Factory.getInstance().newEditable(spannableString)

        if (editableText.isNotEmpty()) {
            binding.inputMessage.text = editableText
        } else {
            Toast.makeText(this, "Failed to display image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessage() {
        val message = HashMap<String, Any>()
        message[Constants.KEY_SENDER_ID] = preferenceManager.getString(Constants.KEY_USER_ID)!!
        message[Constants.KEY_RECEIVER_ID] = receiverUser.id
        message[Constants.KEY_TIMESTAMP] = Date()

        val messageText = binding.inputMessage.text.toString()
        val isImage = binding.inputMessage.text.getSpans(0, messageText.length, ImageSpan::class.java).isNotEmpty()

        if (isImage && base64ImageString != null) {
            message[Constants.KEY_MESSAGE_TYPE] = "image"
            message[Constants.KEY_MESSAGE] = base64ImageString!!
        } else {
            message[Constants.KEY_MESSAGE_TYPE] = "text"
            message[Constants.KEY_MESSAGE] = messageText
        }

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message)
        if (conversionId != null) {
            updateConversion(if (isImage) "[Image]" else messageText)
        } else {
            val conversion = HashMap<String, Any>().apply {
                put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID)!!)
                put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME)!!)
                put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE)!!)
                put(Constants.KEY_RECEIVER_ID, receiverUser.id)
                put(Constants.KEY_RECEIVER_NAME, receiverUser.name)
                put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image)
                put(Constants.KEY_LAST_MESSAGE, if (isImage) "[Image]" else messageText)
                put(Constants.KEY_TIMESTAMP, Date())
            }
            addConversion(conversion)
        }
        binding.inputMessage.text = null
        base64ImageString = null // Reset the image string after sending
    }

    private fun listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS)
            .document(receiverUser.id)
            .addSnapshotListener(this@ChatActivity) { value, error ->
                if(error != null) {
                    return@addSnapshotListener
                }
                value?.let{
                    val availability = it.getLong(Constants.KEY_AVAILABILITY)?.toInt()
                    isReceiverAvailable = availability == 1
                    receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN).toString()
                }
                if(isReceiverAvailable) {
                    binding.textAvailability.visibility = View.VISIBLE
                } else {
                    binding.textAvailability.visibility = View.GONE
                }
            }
    }

    private fun listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
            .addSnapshotListener(eventListener)
        database.collection(Constants.KEY_COLLECTION_CHAT)
            .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID)!!)
            .addSnapshotListener(eventListener)
    }

    @SuppressLint("NotifyDataSetChanged")
    private var eventListener = EventListener<QuerySnapshot> { value, error ->
        if (error != null) {
            return@EventListener
        }
        if (value != null) {
            val count = chatMessages.size
            for (documentChange in value.documentChanges) {
                if (documentChange.type == DocumentChange.Type.ADDED) {
                    val chatMessage = ChatMessage().apply {
                        messageId = documentChange.document.id
                        senderId = documentChange.document.getString(Constants.KEY_SENDER_ID).toString()
                        receiverId = documentChange.document.getString(Constants.KEY_RECEIVER_ID).toString()
                        message = documentChange.document.getString(Constants.KEY_MESSAGE).toString()
                        dateTime = getReadableDateTime(documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!)
                        dateObject = documentChange.document.getDate(Constants.KEY_TIMESTAMP)!!
                        messageType = documentChange.document.getString(Constants.KEY_MESSAGE_TYPE).toString() // Set messageType
                    }
                    chatMessages.add(chatMessage)
                }
            }
            chatMessages.sortWith { obj1, obj2 -> obj1.dateObject.compareTo(obj2.dateObject) }
            if (count == 0) {
                chatAdapter.notifyDataSetChanged()
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size, chatMessages.size)
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
            }
            binding.chatRecyclerView.visibility = View.VISIBLE
        }
        binding.progressBar.visibility = View.GONE
        if (conversionId == null) {
            checkForConversion()
        }
    }


    private fun getBitmapEncodedString(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun loadReceiverDetails() {
        receiverUser = intent.getSerializableExtra(Constants.KEY_USER) as User
        binding.textName.text = receiverUser.name
    }

    @SuppressLint("SimpleDateFormat")
    private fun getReadableDateTime(date: Date): String {
        return SimpleDateFormat("MMMM dd, yyyy - hh:mm a").format(date)
    }
    private val conversionOnCompleteListener = OnCompleteListener<QuerySnapshot> { task ->
        if (task.isSuccessful && task.result != null && task.result.documents.size > 0) {
            val documentSnapshot = task.result.documents[0]
            conversionId = documentSnapshot.id
        }
    }

    private fun addConversion(conversation: HashMap<String, Any>) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .add(conversation)
            .addOnSuccessListener { documentReference ->
                conversionId = documentReference.id
            }
    }

    private fun updateConversion(message: String) {
        val documentReference =
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId!!)
        documentReference.update(
            Constants.KEY_LAST_MESSAGE, message,
            Constants.KEY_TIMESTAMP, Date()
        )
    }


    private fun checkForConversionRemotely(senderId: String, receiverId: String) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
            .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
            .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
            .get()
            .addOnCompleteListener(conversionOnCompleteListener)
    }
    private fun checkForConversion() {
        if (chatMessages.size != 0) {
            checkForConversionRemotely(
                preferenceManager.getString(Constants.KEY_USER_ID)!!,
                receiverUser.id
            )
            checkForConversionRemotely(
                receiverUser.id,
                preferenceManager.getString(Constants.KEY_USER_ID)!!
            )
        }
    }

    override fun onResume() {
        super.onResume()
        listenAvailabilityOfReceiver()
    }

}