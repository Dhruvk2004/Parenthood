package com.example.boardingscreen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Get_Started: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.get_started, container, false)
        val logInButton: Button = view.findViewById(R.id.log_in_bt)
        logInButton.setOnClickListener {
            goToLogin()
        }

        return view
    }

    private fun goToLogin() {
        val intent = Intent(activity, UserType::class.java)
        startActivity(intent)
    }
}