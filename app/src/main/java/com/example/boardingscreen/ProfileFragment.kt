package com.example.boardingscreen

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class ProfileFragment : Fragment() {

    private lateinit var profileName: AppCompatTextView
    private lateinit var profileEmail: AppCompatTextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileImage: ImageView
    private lateinit var pencil: ImageView
    private lateinit var logout: MaterialCardView
    private lateinit var scanner: GmsBarcodeScanner
    private lateinit var addchild: MaterialCardView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var childIdsList: MutableList<String>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        logout = view.findViewById(R.id.Logout_card)
        addchild = view.findViewById(R.id.addchild_card)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        profileImage = view.findViewById(R.id.profile_image)
        pencil = view.findViewById(R.id.pen_icon)

        val options=intializaGoogleScanner()
        scanner= GmsBarcodeScanning.getClient(requireContext(),options)

        sharedPreferences = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val childIdsJson = sharedPreferences.getString("child_ids", null)
        childIdsList = if (childIdsJson != null) {
            Gson().fromJson(childIdsJson, Array<String>::class.java).toMutableList()
        } else {
            mutableListOf()
        }

        loadGenderFromPreferences()
        loadNameAndEmail()

        pencil.setOnClickListener { view ->
            showPopupMenu(view)
        }

        logout.setOnClickListener {
            showLogoutDialog()
        }

        addchild.setOnClickListener {
            showAddChildDialog()
        }
    }

    private fun intializaGoogleScanner(): GmsBarcodeScannerOptions {
        return GmsBarcodeScannerOptions.Builder().
        setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom().build()
    }

    private fun loadGenderFromPreferences() {
        val gender = sharedPreferences.getString("gender", null)

        when (gender) {
            "Male" -> profileImage.setImageResource(R.drawable.man)
            "Female" -> profileImage.setImageResource(R.drawable.woman)
            else -> profileImage.setImageResource(R.drawable.man)
        }
    }

    // Function to show popup menu for gender selection
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.profile_gen, popupMenu.menu)

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.male -> {
                    profileImage.setImageResource(R.drawable.man)
                    saveGenderToPreferences("Male")
                    true
                }
                R.id.female -> {
                    profileImage.setImageResource(R.drawable.woman)
                    saveGenderToPreferences("Female")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun saveGenderToPreferences(gender: String) {
        val editor = sharedPreferences.edit()
        editor.putString("gender", gender)
        editor.apply()
    }

    private fun loadNameAndEmail() {
        val savedName = sharedPreferences.getString("name", null)
        val savedEmail = sharedPreferences.getString("email", null)

        // Check if name and email are already stored in SharedPreferences
        if (savedName != null && savedEmail != null) {
            // If stored, display them in the TextViews
            profileName.text = savedName
            profileEmail.text = savedEmail
        } else {
            // If not stored, fetch from Firestore and update the UI
            fetchNameAndEmailFromFirestore()
        }
    }

    private fun fetchNameAndEmailFromFirestore() {
        val userId = auth.uid
        if (userId != null) {
            // Fetch the parent document from Firestore
            db.collection("Parent").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name")
                        val email = document.getString("email")

                        if (name != null && email != null) {
                            profileName.text = name
                            profileEmail.text = email
                            saveNameAndEmailToSharedPreferences(name, email)
                        }
                    }
                }
                .addOnFailureListener { e ->
                }
        }
    }

    private fun saveNameAndEmailToSharedPreferences(name: String, email: String) {
        val editor = sharedPreferences.edit()
        editor.putString("name", name)
        editor.putString("email", email)
        editor.apply()
    }

    private fun showLogoutDialog() {
        // Create a new Dialog instance
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.logout_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        val pairButton = dialog.findViewById<AppCompatButton>(R.id.yes_btn)
        val cancelbtn = dialog.findViewById<AppCompatButton>(R.id.cancel_btn)
        pairButton.setOnClickListener {
            // Logic for scanning and pairing
            performLogout()
            dialog.dismiss()
        }
        cancelbtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddChildDialog(){
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.scan_dialog)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set transparent background

        val pairButton = dialog.findViewById<AppCompatButton>(R.id.pair_btn)
        pairButton.setOnClickListener {
            // Logic for scanning and pairing
            addChildDevice()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun addChildDevice() {
        scanner.startScan().addOnSuccessListener { scanResult ->
            val rawValue = scanResult.rawValue
            rawValue?.let { result ->
                // Ensure you're not using a URL or invalid characters as the document ID
                if (!result.contains("http") && !result.contains("//")) {

                    if(childIdsList.contains(result)){
                        Toast.makeText(requireContext(),"Child Profile already connected", Toast.LENGTH_SHORT ).show()
                    }else {
                        // Step 1: Check if the child document exists in the Child collection
                        db.collection("Child")
                            .document(result)
                            .get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (documentSnapshot.exists()) {
                                    // Step 2: If the childId exists, update the Parent's child_ids
                                    db.collection("Parent")
                                        .document(auth.uid.toString())
                                        .update("child_ids", FieldValue.arrayUnion(result))
                                        .addOnSuccessListener {
                                            childIdsList.add(result)
                                            val updatedChildIdsJson = Gson().toJson(childIdsList)
                                            val editor = sharedPreferences.edit()
                                            editor.putString("child_ids", updatedChildIdsJson)
                                            editor.apply()
                                            Toast.makeText(
                                                requireContext(),
                                                "Child Profile added successfully",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.d("homefragment", e.toString())
                                            Toast.makeText(
                                                requireContext(),
                                                "Issue found",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        "Invalid Child Profile",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.d("homefragment", e.toString())
                            }
                    }
                } else {
                    Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_LONG).show()
                }

            }
        }.addOnCanceledListener {
            Toast.makeText(requireContext(), "Scan Cancelled", Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Log.d("homefragment", it.toString())
            Toast.makeText(requireContext(), "Issue found during scan", Toast.LENGTH_LONG).show()
        }
    }


    private fun performLogout() {
        // Clear SharedPreferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        // Sign out from FirebaseAuth
        auth.signOut()

        // Redirect to login screen (assuming you have a LoginActivity)
        val intent = Intent(requireContext(), UserType::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

}