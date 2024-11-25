package com.example.chatapp_firebase.adapters

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.util.Linkify
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp_firebase.databinding.ItemContainerReceivedImageMessageBinding
import com.example.chatapp_firebase.databinding.ItemContainerReceivedMessageBinding
import com.example.chatapp_firebase.databinding.ItemContainerSentImageMessageBinding
import com.example.chatapp_firebase.databinding.ItemContainerSentMessageBinding
import com.example.chatapp_firebase.models.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatAdapter(
    private val chatMessages: MutableList<ChatMessage>,
    private val receiverProfileImage: Bitmap,
    private val senderId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT_TEXT = 1
        private const val VIEW_TYPE_RECEIVED_TEXT = 2
        private const val VIEW_TYPE_SENT_IMAGE = 3
        private const val VIEW_TYPE_RECEIVED_IMAGE = 4
    }
    inner class SentMessageViewHolder(private val binding: ItemContainerSentMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(chatMessage: ChatMessage) {
            binding.textMessage.text = chatMessage.message
            Linkify.addLinks(binding.textMessage, Linkify.WEB_URLS)
            binding.textDateTime.text = chatMessage.dateTime

            binding.root.setOnLongClickListener {
                showDeleteDialog(chatMessage)
                true
            }
        }

        private fun showDeleteDialog(chatMessage: ChatMessage) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteMessage(chatMessage)
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun deleteMessage(chatMessage: ChatMessage) {
            val db = FirebaseFirestore.getInstance()
            db.collection("chat")
                .document(chatMessage.messageId)
                .delete()
                .addOnSuccessListener {
                    val position = chatMessages.indexOf(chatMessage)
                    if (position != -1) {
                        chatMessages.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
        }
    }

    inner class ReceivedMessageViewHolder(private val binding: ItemContainerReceivedMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(chatMessage: ChatMessage, receiverProfileImage: Bitmap) {
            binding.textMessage.text = chatMessage.message
            Linkify.addLinks(binding.textMessage, Linkify.WEB_URLS)
            binding.textDateTime.text = chatMessage.dateTime
            binding.imageProfile.setImageBitmap(receiverProfileImage)
        }
    }

    inner class SentImageMessageViewHolder(private val binding: ItemContainerSentImageMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(chatMessage: ChatMessage) {
            val bytes = Base64.decode(chatMessage.message, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.imageMessage.setImageBitmap(bitmap)
            binding.textDateTime.text = chatMessage.dateTime

            binding.root.setOnLongClickListener {
                showDeleteDialog(chatMessage)
                true
            }
        }

        private fun showDeleteDialog(chatMessage: ChatMessage) {
            AlertDialog.Builder(binding.root.context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Yes") { _, _ ->
                    deleteMessage(chatMessage)
                }
                .setNegativeButton("No", null)
                .show()
        }

        private fun deleteMessage(chatMessage: ChatMessage) {
            val db = FirebaseFirestore.getInstance()
            db.collection("chat")
                .document(chatMessage.messageId)
                .delete()
                .addOnSuccessListener {
                    val position = chatMessages.indexOf(chatMessage)
                    if (position != -1) {
                        chatMessages.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
        }
    }


    inner class ReceivedImageMessageViewHolder(private val binding: ItemContainerReceivedImageMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(chatMessage: ChatMessage, receiverProfileImage: Bitmap) {
            val bytes = Base64.decode(chatMessage.message, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.imageMessage.setImageBitmap(bitmap)
            binding.textDateTime.text = chatMessage.dateTime
            binding.imageProfile.setImageBitmap(receiverProfileImage)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentMessageViewHolder(
                ItemContainerSentMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedMessageViewHolder(
                ItemContainerReceivedMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            VIEW_TYPE_SENT_IMAGE -> SentImageMessageViewHolder(
                ItemContainerSentImageMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            VIEW_TYPE_RECEIVED_IMAGE -> ReceivedImageMessageViewHolder(
                ItemContainerReceivedImageMessageBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatMessage = chatMessages[position]
        when (holder) {
            is SentMessageViewHolder -> holder.setData(chatMessage)
            is ReceivedMessageViewHolder -> holder.setData(chatMessage, receiverProfileImage)
            is SentImageMessageViewHolder -> holder.setData(chatMessage)
            is ReceivedImageMessageViewHolder -> holder.setData(chatMessage, receiverProfileImage)
        }
    }
    override fun getItemViewType(position: Int): Int {
        val chatMessage = chatMessages[position]
        return if (chatMessage.senderId == senderId) {
            if (chatMessage.messageType == "image") VIEW_TYPE_SENT_IMAGE else VIEW_TYPE_SENT_TEXT
        } else {
            if (chatMessage.messageType == "image") VIEW_TYPE_RECEIVED_IMAGE else VIEW_TYPE_RECEIVED_TEXT
        }
    }

}