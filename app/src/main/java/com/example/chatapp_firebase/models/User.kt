package com.example.chatapp_firebase.models

import java.io.Serializable

data class User(
    var name: String,
    var image: String,
    var email: String,
    var token: String,
    var id: String,
    var password: String
) : Serializable
