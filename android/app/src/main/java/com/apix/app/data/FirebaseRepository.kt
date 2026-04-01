package com.apix.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseRepository {

    private const val TAG = "FirebaseRepo"

    suspend fun ensureAnonymousAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
                Log.d(TAG, "Anonymous auth success: ${auth.currentUser?.uid}")
            } catch (e: Exception) {
                Log.e(TAG, "Anonymous auth failed", e)
                throw e
            }
        }
    }

    fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val ref = FirebaseDatabase.getInstance().getReference("categories")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val cats = mutableListOf<Category>()
                for (child in snapshot.children) {
                    try {
                        val cat = child.getValue(Category::class.java) ?: continue
                        cat.id = child.key ?: continue
                        if (cat.hidden) continue

                        val channelsMap = mutableMapOf<String, Channel>()
                        for (chSnap in child.child("channels").children) {
                            val ch = chSnap.getValue(Channel::class.java) ?: continue
                            ch.id = chSnap.key ?: continue
                            channelsMap[ch.id] = ch
                        }
                        cat.channels = channelsMap
                        cats.add(cat)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing category", e)
                    }
                }
                cats.sortBy { it.sortOrder }
                trySend(cats)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Categories cancelled: ${error.message}")
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeSideMenus(): Flow<Map<String, SideMenu>> = callbackFlow {
        val ref = FirebaseDatabase.getInstance().getReference("sideMenus")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val menus = mutableMapOf<String, SideMenu>()
                for (child in snapshot.children) {
                    try {
                        val menu = child.getValue(SideMenu::class.java) ?: continue
                        menu.id = child.key ?: continue
                        menus[menu.id] = menu
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing side menu", e)
                    }
                }
                trySend(menus)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "SideMenus cancelled: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
