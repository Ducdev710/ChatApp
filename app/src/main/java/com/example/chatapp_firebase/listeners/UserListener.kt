package com.example.chatapp_firebase.listeners

import com.example.chatapp_firebase.models.User

interface UserListener {
    fun onUserClicked(user: User)
}