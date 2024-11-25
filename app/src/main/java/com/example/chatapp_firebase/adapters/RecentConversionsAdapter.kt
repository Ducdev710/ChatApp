package com.example.chatapp_firebase.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp_firebase.databinding.ItemContainerRecentConversionBinding
import com.example.chatapp_firebase.listeners.ConversionListener
import com.example.chatapp_firebase.models.ChatMessage
import com.example.chatapp_firebase.models.User

class RecentConversationsAdapter(
    private var chatMessages: MutableList<ChatMessage>,
    private val conversionListener: ConversionListener,
    private val currentUserId: String
) : RecyclerView.Adapter<RecentConversationsAdapter.ConversionViewHolder>() {

    inner class ConversionViewHolder(private val binding: ItemContainerRecentConversionBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun setData(chatMessage: ChatMessage) {
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage))
            binding.textName.text = chatMessage.conversionName
            /*val sender = if (chatMessage.lastMessageSenderId == currentUserId) "You" else chatMessage.senderName
            binding.textRecentMessage.text = "$sender: ${chatMessage.message}"
            if (!chatMessage.isSeen && chatMessage.senderId != currentUserId) {
                binding.imageUnseenMessage.visibility = View.VISIBLE
            } else {
                binding.imageUnseenMessage.visibility = View.GONE
            }*/
            binding.textRecentMessage.text = chatMessage.message
            binding.root.setOnClickListener {
                val user = User(
                    id = chatMessage.conversionId,
                    name = chatMessage.conversionName,
                    image = chatMessage.conversionImage,
                    email = "",
                    token = "",
                    password = ""
                )
                conversionListener.onConversionClicked(user)
            }
        }
    }

    private fun getConversionImage(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversionViewHolder {
        return ConversionViewHolder(
            ItemContainerRecentConversionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    override fun onBindViewHolder(holder: ConversionViewHolder, position: Int) {
        holder.setData(chatMessages[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newConversations: List<ChatMessage>) {
        chatMessages.clear()
        chatMessages.addAll(newConversations)
        notifyDataSetChanged()
    }
}