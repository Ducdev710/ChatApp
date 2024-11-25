package com.example.chatapp_firebase.activities

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.chatapp_firebase.R
import com.example.chatapp_firebase.databinding.DialogUserInfoBinding
import com.example.chatapp_firebase.models.User

class UserInfoDialogFragment(private val user: User, private val isOnline: Boolean) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogUserInfoBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(binding.root)

        binding.textName.text = user.name
        binding.textStatus.text = if (isOnline) "Online" else "Offline"

        if (user.image.isNotEmpty()) {
            val imageBytes = Base64.decode(user.image, Base64.DEFAULT)
            if (imageBytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                binding.imageProfile.setImageBitmap(bitmap)
            } else {
                binding.imageProfile.setImageResource(R.drawable.baseline_info_24)
            }
        } else {
            binding.imageProfile.setImageResource(R.drawable.baseline_info_24)
        }

        /*dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.66).toInt(),
            (resources.displayMetrics.heightPixels * 0.5).toInt()
        )*/

        return dialog
    }
}