package com.example.boardingscreen.data.repository

import com.example.boardingscreen.data.model.Child
import com.example.boardingscreen.utils.Constants
import com.example.boardingscreen.utils.Result
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ChildRepositoryImpl : ChildRepository {
    
    private val db = FirebaseFirestore.getInstance()
    
    override suspend fun getChild(childId: String): Result<Child> {
        return try {
            val document = db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .get()
                .await()
            
            if (document.exists()) {
                val childName = document.getString("name")
                if (childName != null) {
                    Result.Success(Child(id = childId, name = childName))
                } else {
                    Result.Error(Exception(Constants.ERROR_CHILD_NOT_FOUND))
                }
            } else {
                Result.Error(Exception(Constants.ERROR_CHILD_NOT_FOUND))
            }
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_UNKNOWN}: ${e.message}"))
        }
    }
    
    override suspend fun getChildLocation(childId: String): Result<Pair<Double, Double>> {
        return try {
            val childDocument = db.collection(Constants.COLLECTION_CHILD)
                .document(childId)
                .get()
                .await()
            
            if (childDocument.exists()) {
                val location = childDocument.getGeoPoint("location")
                if (location != null) {
                    Result.Success(Pair(location.latitude, location.longitude))
                } else {
                    Result.Error(Exception(Constants.ERROR_LOCATION_NOT_FOUND))
                }
            } else {
                Result.Error(Exception(Constants.ERROR_CHILD_NOT_FOUND))
            }
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_UNKNOWN}: ${e.message}"))
        }
    }
    
    override suspend fun getChildrenForParent(parentId: String): Result<List<Child>> {
        return try {
            // Fetch parent document to get child_ids array
            val parentDocument = db.collection(Constants.COLLECTION_PARENT)
                .document(parentId)
                .get()
                .await()
            
            if (parentDocument.exists()) {
                val childIds = parentDocument.get("child_ids") as? List<String>
                
                if (childIds.isNullOrEmpty()) {
                    // No children connected - return empty list
                    Result.Success(emptyList())
                } else {
                    // Fetch all child documents
                    val childList = mutableListOf<Child>()
                    
                    for (childId in childIds) {
                        try {
                            val childDocument = db.collection(Constants.COLLECTION_CHILD)
                                .document(childId)
                                .get()
                                .await()
                            
                            if (childDocument.exists()) {
                                val childName = childDocument.getString("name")
                                if (childName != null) {
                                    childList.add(Child(id = childId, name = childName))
                                }
                            }
                        } catch (e: Exception) {
                            // Log individual child fetch failure but continue with others
                            continue
                        }
                    }
                    
                    Result.Success(childList)
                }
            } else {
                Result.Error(Exception("Parent document not found"))
            }
        } catch (e: Exception) {
            Result.Error(Exception("${Constants.ERROR_UNKNOWN}: ${e.message}"))
        }
    }
}
