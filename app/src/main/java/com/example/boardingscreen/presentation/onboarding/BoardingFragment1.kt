package com.example.boardingscreen.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.boardingscreen.R
import com.example.boardingscreen.presentation.auth.LoginActivity

class BoardingFragment1: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.on_boarding_fragment1, container, false)

        val skipTextView: TextView? = view.findViewById(R.id.skip_bt)


        // Set an OnClickListener on the skip TextView
        skipTextView?.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
        return view
    }
}