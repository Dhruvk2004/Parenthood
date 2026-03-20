package com.example.boardingscreen.data.repository

import com.example.boardingscreen.utils.Result

/**
 * Repository interface for authentication-related operations.
 * Provides methods for user login, signup, logout, and session management.
 */
interface AuthRepository {
    /**
     * Authenticates a user with email and password.
     * @param email User's email address
     * @param password User's password
     * @return Result containing the user ID on success or an error
     */
    suspend fun login(email: String, password: String): Result<String>
    
    /**
     * Creates a new user account.
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @param userType Type of user (Parent or Child)
     * @return Result containing the new user ID on success or an error
     */
    suspend fun signup(email: String, password: String, name: String, userType: String): Result<String>
    
    /**
     * Signs out the current user.
     * @return Result indicating success or an error
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Gets the current authenticated user's ID.
     * @return The user ID if authenticated, null otherwise
     */
    suspend fun getCurrentUserId(): String?
}
