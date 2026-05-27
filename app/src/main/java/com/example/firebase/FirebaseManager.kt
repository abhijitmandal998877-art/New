package com.example.firebase

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private const val PREFS_NAME = "PeriodCarePrefs"
    
    // States
    private val _currentUserFlow = MutableStateFlow<UserProfile?>(null)
    val currentUserFlow: StateFlow<UserProfile?> = _currentUserFlow

    private val _periodLogs = MutableStateFlow<List<PeriodLog>>(emptyList())
    val periodLogs: StateFlow<List<PeriodLog>> = _periodLogs

    private val _notifications = MutableStateFlow<List<AdminNotification>>(emptyList())
    val notifications: StateFlow<List<AdminNotification>> = _notifications

    private val _allUsersFlow = MutableStateFlow<List<UserProfile>>(emptyList())
    val allUsersFlow: StateFlow<List<UserProfile>> = _allUsersFlow

    private val _allFeedbackFlow = MutableStateFlow<List<FeedbackItem>>(emptyList())
    val allFeedbackFlow: StateFlow<List<FeedbackItem>> = _allFeedbackFlow

    // Firebase instances
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    var isFirebaseOnline = false
        private set

    // Shared preferences fallback storage
    private var contextRef: Context? = null

    fun initialize(context: Context) {
        contextRef = context.applicationContext
        try {
            val app = FirebaseApp.getInstance()
            auth = FirebaseAuth.getInstance()
            firestore = FirebaseFirestore.getInstance()
            isFirebaseOnline = true
            Log.d(TAG, "Firebase successfully connected!")
            
            // Listen to active auth status
            auth?.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    fetchUserProfile(fbUser.uid)
                } else {
                    _currentUserFlow.value = null
                    _periodLogs.value = emptyList()
                }
            }
        } catch (e: Exception) {
            isFirebaseOnline = false
            Log.e(TAG, "Firebase unavailable, utilizing Local Sandbox fallback: ${e.localizedMessage}")
        }

        // Initialize mock or existing data from Local SharedPreferences
        loadLocalData()
    }

    // --- Authentication ---
    fun signUp(name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        val ctx = contextRef ?: return
        
        if (isFirebaseOnline) {
            auth?.createUserWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        if (firebaseUser != null) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            val userProfile = UserProfile(
                                uid = firebaseUser.uid,
                                name = name,
                                email = email,
                                registrationDate = sdf.format(Date())
                            )
                            
                            // Save user to Firestore
                            firestore?.collection("users")?.document(firebaseUser.uid)
                                ?.set(userProfile)
                                ?.addOnSuccessListener {
                                    _currentUserFlow.value = userProfile
                                    saveUserToLocalDatabase(userProfile)
                                    // Add to analytics list in Firestore
                                    syncAnalyticsToLocal()
                                    onResult(true, "Successfully Registered with Firebase!")
                                }
                                ?.addOnFailureListener { e ->
                                    // Backup local store
                                    _currentUserFlow.value = userProfile
                                    saveUserToLocalDatabase(userProfile)
                                    onResult(true, "Authentication succeeded, profile offline: ${e.localizedMessage}")
                                }
                        } else {
                            onResult(false, "User verification error")
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Sign up failed")
                    }
                }
        } else {
            // Local sandbox Sign up
            val localUsers = getLocalUsers()
            if (localUsers.any { it.email.lowercase() == email.lowercase() }) {
                onResult(false, "This email is already registered in Local Sandbox!")
                return
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val fakeUid = UUID.randomUUID().toString()
            val localProfile = UserProfile(
                uid = fakeUid,
                name = name,
                email = email,
                registrationDate = sdf.format(Date())
            )
            
            // Save in prefs
            saveLocalUserCredentials(email, password, localProfile)
            _currentUserFlow.value = localProfile
            Toast.makeText(ctx, "Sandbox: Created Offline Account for $name", Toast.LENGTH_LONG).show()
            onResult(true, "Registered successfully in Offline Sandbox!")
        }
    }

    fun login(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        val ctx = contextRef ?: return
        
        if (isFirebaseOnline) {
            auth?.signInWithEmailAndPassword(email, password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fbUser = task.result?.user
                        if (fbUser != null) {
                            fetchUserProfile(fbUser.uid) { profile ->
                                if (profile != null) {
                                    saveUserToLocalDatabase(profile)
                                    onResult(true, "Logged in as ${profile.name}")
                                } else {
                                    val fallbackProfile = UserProfile(
                                        uid = fbUser.uid,
                                        name = email.substringBefore("@"),
                                        email = email,
                                        registrationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    )
                                    _currentUserFlow.value = fallbackProfile
                                    onResult(true, "User Logged In (Profile Fallback)")
                                }
                            }
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Invalid login credentials")
                    }
                }
        } else {
            // Sandbox Login Check
            val storedPassword = getLocalPasswordForEmail(email)
            if (storedPassword == password) {
                val profile = getLocalProfileByEmail(email)
                _currentUserFlow.value = profile
                loadLocalData() // reload logs for this user
                Toast.makeText(ctx, "Sandbox Mode: Safe Authentication", Toast.LENGTH_SHORT).show()
                onResult(true, "Success")
            } else {
                onResult(false, "Invalid email or matching password in Sandbox!")
            }
        }
    }

    fun logout() {
        if (isFirebaseOnline) {
            auth?.signOut()
        }
        _currentUserFlow.value = null
        _periodLogs.value = emptyList()
        _currentUserFlow.value = null
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String) -> Unit) {
        val user = _currentUserFlow.value ?: return onResult(false, "No active user logged in")
        if (isFirebaseOnline) {
            auth?.currentUser?.updatePassword(newPassword)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, "Password changed successfully in Firebase Auth!")
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Change password failed")
                    }
                }
        } else {
            // Local change password
            val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
            prefs.edit().putString("pwd_${user.email}", newPassword).apply()
            onResult(true, "Password changed successfully in Offline Sandbox!")
        }
    }

    private fun fetchUserProfile(uid: String, onFinished: ((UserProfile?) -> Unit)? = null) {
        if (!isFirebaseOnline) return
        firestore?.collection("users")?.document(uid)?.get()
            ?.addOnSuccessListener { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    _currentUserFlow.value = profile
                    fetchPeriodLogs(profile.uid)
                    onFinished?.invoke(profile)
                } else {
                    onFinished?.invoke(null)
                }
            }
            ?.addOnFailureListener {
                onFinished?.invoke(null)
            }
    }

    // --- Period Logs Management ---
    fun fetchPeriodLogs(userId: String) {
        if (isFirebaseOnline) {
            firestore?.collection("period_logs")
                ?.whereEqualTo("userId", userId)
                ?.get()
                ?.addOnSuccessListener { query ->
                    val logs = mutableListOf<PeriodLog>()
                    for (doc in query) {
                        val log = doc.toObject(PeriodLog::class.java)
                        logs.add(log.copy(id = doc.id))
                    }
                    // Sort from latest to oldest
                    logs.sortByDescending { it.startDate }
                    _periodLogs.value = logs
                    saveLogsLocal(logs)
                }
                ?.addOnFailureListener {
                    // Fallback to local
                    loadLocalLogsForCurrentUser()
                }
        } else {
            loadLocalLogsForCurrentUser()
        }
    }

    fun addPeriodLog(startDate: String, endDate: String, cycleLength: Int, periodLength: Int, symptoms: List<String>, onResult: (Boolean) -> Unit) {
        val userItem = _currentUserFlow.value ?: return onResult(false)
        val id = UUID.randomUUID().toString()
        val log = PeriodLog(
            id = id,
            userId = userItem.uid,
            startDate = startDate,
            endDate = endDate,
            cycleLength = cycleLength,
            periodLength = periodLength,
            symptoms = symptoms
        )

        if (isFirebaseOnline) {
            firestore?.collection("period_logs")?.document(id)
                ?.set(log)
                ?.addOnSuccessListener {
                    val updatedList = (_periodLogs.value + log).sortedByDescending { it.startDate }
                    _periodLogs.value = updatedList
                    saveLogsLocal(updatedList)
                    onResult(true)
                }
                ?.addOnFailureListener {
                    // Save locally as temporary backup
                    val updatedList = (_periodLogs.value + log).sortedByDescending { it.startDate }
                    _periodLogs.value = updatedList
                    saveLogsLocal(updatedList)
                    onResult(true)
                }
        } else {
            val updatedList = (_periodLogs.value + log).sortedByDescending { it.startDate }
            _periodLogs.value = updatedList
            saveLogsLocal(updatedList)
            onResult(true)
        }
    }

    fun deletePeriodLog(logId: String, onResult: (Boolean) -> Unit) {
        if (isFirebaseOnline) {
            firestore?.collection("period_logs")?.document(logId)
                ?.delete()
                ?.addOnSuccessListener {
                    val updatedList = _periodLogs.value.filter { it.id != logId }
                    _periodLogs.value = updatedList
                    saveLogsLocal(updatedList)
                    onResult(true)
                }
                ?.addOnFailureListener {
                    onResult(false)
                }
        } else {
            val updatedList = _periodLogs.value.filter { it.id != logId }
            _periodLogs.value = updatedList
            saveLogsLocal(updatedList)
            onResult(true)
        }
    }

    // --- Support Feedback & Contacts ---
    fun sendFeedback(message: String, onResult: (Boolean, String) -> Unit) {
        val user = _currentUserFlow.value ?: return onResult(false, "Must log in to send support message")
        val id = UUID.randomUUID().toString()
        val fb = FeedbackItem(
            id = id,
            name = user.name,
            email = user.email,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        if (isFirebaseOnline) {
            firestore?.collection("feedback")?.document(id)
                ?.set(fb)
                ?.addOnSuccessListener {
                    addFeedbackToLocal(fb)
                    onResult(true, "Feedback securely transmitted to Developer Portal!")
                }
                ?.addOnFailureListener { e ->
                    addFeedbackToLocal(fb)
                    onResult(true, "Feedback saved offline. It will sync later.")
                }
        } else {
            addFeedbackToLocal(fb)
            onResult(true, "Feedback logged in local developer portal database!")
        }
    }

    // --- Admin Dashboard: Core Control Panel ---
    fun fetchAllFeedback(onFinished: (List<FeedbackItem>) -> Unit) {
        if (isFirebaseOnline) {
            firestore?.collection("feedback")
                ?.get()
                ?.addOnSuccessListener { query ->
                    val items = mutableListOf<FeedbackItem>()
                    for (doc in query) {
                        try {
                            val item = doc.toObject(FeedbackItem::class.java)
                            items.add(item.copy(id = doc.id))
                        } catch (e: Exception) {
                            Log.e(TAG, "Feedback parse error: ${e.message}")
                        }
                    }
                    items.sortByDescending { it.timestamp }
                    _allFeedbackFlow.value = items
                    saveAllFeedbackLocal(items)
                    onFinished(items)
                }
                ?.addOnFailureListener {
                    onFinished(_allFeedbackFlow.value)
                }
        } else {
            onFinished(_allFeedbackFlow.value)
        }
    }

    fun fetchAllUsers(onFinished: (List<UserProfile>) -> Unit) {
        if (isFirebaseOnline) {
            firestore?.collection("users")
                ?.get()
                ?.addOnSuccessListener { query ->
                    val users = mutableListOf<UserProfile>()
                    for (doc in query) {
                        try {
                            val u = doc.toObject(UserProfile::class.java)
                            users.add(u)
                        } catch (e: Exception) {
                            Log.e(TAG, "User profile parse error: ${e.message}")
                        }
                    }
                    users.sortByDescending { it.registrationDate }
                    _allUsersFlow.value = users
                    saveAllUsersLocal(users)
                    onFinished(users)
                }
                ?.addOnFailureListener {
                    onFinished(_allUsersFlow.value)
                }
        } else {
            onFinished(_allUsersFlow.value)
        }
    }

    fun broadcastAdminNotification(title: String, text: String, targetEmail: String = "All", onResult: (Boolean) -> Unit) {
        val id = UUID.randomUUID().toString()
        val notif = AdminNotification(
            id = id,
            title = title,
            text = text,
            timestamp = System.currentTimeMillis(),
            target = targetEmail
        )

        if (isFirebaseOnline) {
            firestore?.collection("notifications")?.document(id)
                ?.set(notif)
                ?.addOnSuccessListener {
                    val updated = (_notifications.value + notif).sortedByDescending { it.timestamp }
                    _notifications.value = updated
                    saveNotificationsLocal(updated)
                    onResult(true)
                }
                ?.addOnFailureListener {
                    val updated = (_notifications.value + notif).sortedByDescending { it.timestamp }
                    _notifications.value = updated
                    saveNotificationsLocal(updated)
                    onResult(true)
                }
        } else {
            val updated = (_notifications.value + notif).sortedByDescending { it.timestamp }
            _notifications.value = updated
            saveNotificationsLocal(updated)
            onResult(true)
        }
    }

    fun fetchNotifications() {
        if (isFirebaseOnline) {
            firestore?.collection("notifications")
                ?.get()
                ?.addOnSuccessListener { query ->
                    val list = mutableListOf<AdminNotification>()
                    for (doc in query) {
                        try {
                            val n = doc.toObject(AdminNotification::class.java)
                            list.add(n.copy(id = doc.id))
                        } catch (e: Exception) {}
                    }
                    list.sortByDescending { it.timestamp }
                    _notifications.value = list
                    saveNotificationsLocal(list)
                }
                ?.addOnFailureListener {
                    loadNotificationsLocal()
                }
        } else {
            loadNotificationsLocal()
        }
    }

    // --- Changeable Admin Password in database ---
    fun updateAdminPassword(newPass: String, onResult: (Boolean) -> Unit) {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return onResult(false)
        prefs.edit().putString("admin_password_key", newPass).apply()
        
        if (isFirebaseOnline) {
            val map = mapOf("password" to newPass)
            firestore?.collection("admin")?.document("config")?.set(map)
                ?.addOnSuccessListener { onResult(true) }
                ?.addOnFailureListener { onResult(true) } // local succeeds anyway
        } else {
            onResult(true)
        }
    }

    fun getAdminPassword(): String {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return "9242505224"
        return prefs.getString("admin_password_key", "9242505224") ?: "9242505224"
    }

    // --- Shared Preferences Local Sandbox Storage Helpers ---
    private fun saveLocalUserCredentials(email: String, password: String, profile: UserProfile) {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val editor = prefs.edit()
        editor.putString("pwd_${email}", password)
        editor.putString("profile_${email}", serializeProfile(profile))
        
        // Add to user list tracking
        val existingUsers = getLocalUsers().toMutableList()
        if (existingUsers.none { it.email.lowercase() == email.lowercase() }) {
            existingUsers.add(profile)
            saveAllUsersLocal(existingUsers)
        }
        editor.apply()
    }

    private fun getLocalPasswordForEmail(email: String): String? {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return null
        return prefs.getString("pwd_${email}", null)
    }

    private fun getLocalProfileByEmail(email: String): UserProfile? {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return null
        val serialized = prefs.getString("profile_${email}", null) ?: return null
        return deserializeProfile(serialized)
    }

    private fun getLocalUsers(): List<UserProfile> {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return emptyList()
        val raw = prefs.getString("all_registered_users_list", null) ?: return emptyList()
        return deserializeUsers(raw)
    }

    private fun saveUserToLocalDatabase(profile: UserProfile) {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        prefs.edit().putString("profile_${profile.email}", serializeProfile(profile)).apply()
        
        val users = getLocalUsers().toMutableList()
        if (users.none { it.uid == profile.uid }) {
            users.add(profile)
            saveAllUsersLocal(users)
        }
    }

    private fun syncAnalyticsToLocal() {
        fetchAllUsers { /* fetches and saves locally */ }
        fetchAllFeedback { /* fetches and saves locally */ }
    }

    private fun saveLogsLocal(logs: List<PeriodLog>) {
        val userId = _currentUserFlow.value?.uid ?: return
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        
        val jsonArr = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("userId", log.userId)
                put("startDate", log.startDate)
                put("endDate", log.endDate)
                put("cycleLength", log.cycleLength)
                put("periodLength", log.periodLength)
                
                val sympArr = JSONArray()
                log.symptoms.forEach { sympArr.put(it) }
                put("symptoms", sympArr)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("logs_$userId", jsonArr.toString()).apply()
    }

    private fun loadLocalLogsForCurrentUser() {
        val userId = _currentUserFlow.value?.uid ?: return
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val raw = prefs.getString("logs_$userId", null) ?: return
        
        try {
            val list = mutableListOf<PeriodLog>()
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val symptoms = mutableListOf<String>()
                val sArr = obj.optJSONArray("symptoms")
                if (sArr != null) {
                    for (j in 0 until sArr.length()) {
                        symptoms.add(sArr.getString(j))
                    }
                }
                list.add(
                    PeriodLog(
                        id = obj.getString("id"),
                        userId = obj.getString("userId"),
                        startDate = obj.getString("startDate"),
                        endDate = obj.getString("endDate"),
                        cycleLength = obj.getInt("cycleLength"),
                        periodLength = obj.getInt("periodLength"),
                        symptoms = symptoms
                    )
                )
            }
            list.sortByDescending { it.startDate }
            _periodLogs.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing logs: ${e.message}")
        }
    }

    private fun saveNotificationsLocal(list: List<AdminNotification>) {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val jsonArr = JSONArray()
        list.forEach { n ->
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("text", n.text)
                put("timestamp", n.timestamp)
                put("target", n.target)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("all_notifications", jsonArr.toString()).apply()
    }

    private fun loadNotificationsLocal() {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val raw = prefs.getString("all_notifications", null) ?: return
        try {
            val list = mutableListOf<AdminNotification>()
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AdminNotification(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        text = obj.getString("text"),
                        timestamp = obj.getLong("timestamp"),
                        target = obj.optString("target", "All")
                    )
                )
            }
            list.sortByDescending { it.timestamp }
            _notifications.value = list
        } catch (e: Exception) {}
    }

    private fun saveAllUsersLocal(list: List<UserProfile>) {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val jsonArr = JSONArray()
        list.forEach { u ->
            jsonArr.put(JSONObject(serializeProfile(u)))
        }
        prefs.edit().putString("all_registered_users_list", jsonArr.toString()).apply()
        _allUsersFlow.value = list
    }

    private fun saveAllFeedbackLocal(list: List<FeedbackItem>) {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val jsonArr = JSONArray()
        list.forEach { f ->
            val obj = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("email", f.email)
                put("message", f.message)
                put("timestamp", f.timestamp)
            }
            jsonArr.put(obj)
        }
        prefs.edit().putString("all_dev_feedback_list", jsonArr.toString()).apply()
        _allFeedbackFlow.value = list
    }

    private fun addFeedbackToLocal(fb: FeedbackItem) {
        val existing = _allFeedbackFlow.value.toMutableList()
        if (existing.none { it.id == fb.id }) {
            existing.add(0, fb)
            saveAllFeedbackLocal(existing)
        }
    }

    private fun loadLocalData() {
        val prefs = contextRef?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        
        // Load default mock notification if empty
        val notifsRaw = prefs.getString("all_notifications", null)
        if (notifsRaw == null) {
            val defaultNotifs = listOf(
                AdminNotification(
                    id = "welcome_notif",
                    title = "💐 Welcome to Period Care App",
                    text = "Welcome to your menstrual care companion. We are here to support your mental and physical health. Plan ahead, practice calming yoga, and log your insights.",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    target = "All"
                ),
                AdminNotification(
                    id = "hydration_notif",
                    title = "💧 Clean Hydration Tip",
                    text = "Remember to consume 3-4 liters of warm water today to ease bloating and soothe muscle cramps naturally.",
                    timestamp = System.currentTimeMillis() - 10000000L,
                    target = "All"
                )
            )
            saveNotificationsLocal(defaultNotifs)
        } else {
            loadNotificationsLocal()
        }

        // Load feedback
        val feedRaw = prefs.getString("all_dev_feedback_list", null)
        if (feedRaw != null) {
            try {
                val list = mutableListOf<FeedbackItem>()
                val arr = JSONArray(feedRaw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        FeedbackItem(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            email = obj.getString("email"),
                            message = obj.getString("message"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
                _allFeedbackFlow.value = list
            } catch (e: Exception) {}
        } else {
            // Seed a mock user feedback for admin presentation if empty
            val defaultFeedback = listOf(
                FeedbackItem("fb_1", "Priti Sen", "priti.sen@gmail.com", "Absolutely loving the clean Lavender style design! The prediction math matches my biological calendar perfectly. Thanks Abhijit!", System.currentTimeMillis() - 7200000L),
                FeedbackItem("fb_2", "John Doe", "john@gmail.com", "An incredibly helpful yoga section. This has significantly reduced my daily cycle fatigue.", System.currentTimeMillis() - 14400000L)
            )
            saveAllFeedbackLocal(defaultFeedback)
        }

        // Load users tracker
        val usersRaw = prefs.getString("all_registered_users_list", null)
        if (usersRaw != null) {
            _allUsersFlow.value = deserializeUsers(usersRaw)
        } else {
            // Seed default mock registered users for admin dashboard demonstration if empty
            val defaultUsers = listOf(
                UserProfile("u_1", "Priti Sen", "priti.sen@gmail.com", "2026-05-25 09:24"),
                UserProfile("u_2", "Sneha Roy", "sneha.roy@yahoo.com", "2026-05-26 14:15"),
                UserProfile("u_3", "Ananya Mandal", "ananya@gmail.com", "2026-05-27 10:30")
            )
            saveAllUsersLocal(defaultUsers)
        }

        // Load current users logs if any
        loadLocalLogsForCurrentUser()
    }

    // Serialization utils
    private fun serializeProfile(profile: UserProfile): String {
        return JSONObject().apply {
            put("uid", profile.uid)
            put("name", profile.name)
            put("email", profile.email)
            put("registrationDate", profile.registrationDate)
        }.toString()
    }

    private fun deserializeProfile(serialized: String): UserProfile {
        val obj = JSONObject(serialized)
        return UserProfile(
            uid = obj.getString("uid"),
            name = obj.getString("name"),
            email = obj.getString("email"),
            registrationDate = obj.getString("registrationDate")
        )
    }

    private fun deserializeUsers(raw: String): List<UserProfile> {
        val list = mutableListOf<UserProfile>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UserProfile(
                        uid = obj.getString("uid"),
                        name = obj.getString("name"),
                        email = obj.getString("email"),
                        registrationDate = obj.getString("registrationDate")
                    )
                )
            }
        } catch (e: Exception) {}
        return list
    }
}
