package com.example.boardingscreen.data.repository

import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl : AuthRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    override suspend fun login(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
            
            if (userId != null) {
                Result.Success(userId)
            } else {
                Result.Error(Exception(Constants.ERROR_AUTH_FAILED))
            }
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_INVALID_CREDENTIALS}: ${e.message}"))
        }
    }
    
    override suspend fun signup(email: String, password: String, name: String, userType: String): Result<String> {
        return try {
            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid
            
            if (userId != null) {
                // Store user data in Firestore
                val userMap = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "pass" to password
                )
                
                db.collection(userType).document(userId).set(userMap).await()
                
                Result.Success(userId)
            } else {
                Result.Error(Exception(Constants.ERROR_ACCOUNT_CREATION_FAILED))
            }
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_ACCOUNT_CREATION_FAILED}: ${e.message}"))
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_LOGOUT_FAILED}: ${e.message}"))
        }
    }
    
    override suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
