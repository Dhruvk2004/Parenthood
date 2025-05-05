package com.example.boardingscreen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class BoardingFragment2: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.on_boarding_fragment2, container, false)

        val skipTextView: TextView? = view.findViewById(R.id.skip_bt)


        // Set an OnClickListener on the skip TextView
        skipTextView?.setOnClickListener {
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
        }
        return view
    }


}