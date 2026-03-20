package com.example.boardingscreen.data.repository

import com.example.boardingscreen.data.model.Child
import com.example.boardingscreen.utils.Result

/**
 * Repository interface for child-related data operations.
 * Provides methods to fetch child information and location data from Firestore.
 */
interface ChildRepository {
    /**
     * Fetches a child's profile by their ID.
     * @param childId The unique identifier of the child
     * @return Result containing the Child object or an error
     */
    suspend fun getChild(childId: String): Result<Child>
    
    /**
     * Fetches the current location of a child.
     * @param childId The unique identifier of the child
     * @return Result containing a Pair of (latitude, longitude) or an error
     */
    suspend fun getChildLocation(childId: String): Result<Pair<Double, Double>>
    
    /**
     * Fetches all children connected to a parent.
     * @param parentId The unique identifier of the parent
     * @return Result containing a list of Child objects or an error
     */
    suspend fun getChildrenForParent(parentId: String): Result<List<Child>>
}
