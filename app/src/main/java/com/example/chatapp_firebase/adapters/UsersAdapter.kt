package com.example.chatapp_firebase.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp_firebase.databinding.ItemContainerUserBinding
import com.example.chatapp_firebase.listeners.UserListener
import com.example.chatapp_firebase.models.User

class UsersAdapter(
    private var users: List<User>,
    private val userListener: UserListener
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {


    inner class UserViewHolder(private val binding: ItemContainerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setUserData(user: User) {
            binding.textName.text = user.name
            binding.textEmail.text = user.email
            binding.imageProfile.setImageBitmap(getUserImage(user.image))
            binding.root.setOnClickListener {
                userListener.onUserClicked(user)
            }
        }
    }


    private fun getUserImage(encodedImage: String): Bitmap {
        val bytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val itemContainerUserBinding = ItemContainerUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(itemContainerUserBinding)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.setUserData(users[position])
    }

}
