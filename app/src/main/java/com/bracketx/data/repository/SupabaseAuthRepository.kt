package com.bracketx.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.bracketx.data.network.SupabaseClient
import com.bracketx.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class SupabaseAuthRepository : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var prefs: SharedPreferences? = null

    // Initially null: the app will prompt to sign up or sign in
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun initPreferences(context: Context) {
        prefs = context.getSharedPreferences("bracketx_auth_prefs", Context.MODE_PRIVATE)
        val savedId = prefs?.getString("user_id", null)
        val savedName = prefs?.getString("display_name", null)
        val savedToken = prefs?.getString("auth_token", null)

        if (!savedId.isNullOrBlank() && !savedName.isNullOrBlank()) {
            if (!savedToken.isNullOrBlank()) {
                SupabaseClient.authToken = savedToken
            }
            _currentUser.value = User(id = savedId, displayName = savedName)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        val payload = """{"email":"$email","password":"$password"}"""
        val result = SupabaseClient.post("/auth/v1/token?grant_type=password", payload)

        return result.fold(
            onSuccess = { responseBody ->
                try {
                    val root = json.parseToJsonElement(responseBody).jsonObject
                    val accessToken = root["access_token"]?.jsonPrimitive?.content
                    val userObj = root["user"]?.jsonObject

                    val userId = userObj?.get("id")?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                    val userMeta = userObj?.get("user_metadata")?.jsonObject
                    val displayName = userMeta?.get("display_name")?.jsonPrimitive?.content
                        ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

                    SupabaseClient.authToken = accessToken

                    val user = User(id = userId, displayName = displayName)
                    _currentUser.value = user

                    prefs?.edit()?.apply {
                        putString("user_id", userId)
                        putString("display_name", displayName)
                        putString("auth_token", accessToken)
                        apply()
                    }

                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse login response: ${e.message}"))
                }
            },
            onFailure = { error ->
                val friendlyMessage = extractErrorMessage(error.message)
                Result.failure(Exception(friendlyMessage))
            }
        )
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        val payload = """{"email":"$email","password":"$password","data":{"display_name":"$displayName"}}"""
        val result = SupabaseClient.post("/auth/v1/signup", payload)

        return result.fold(
            onSuccess = { responseBody ->
                try {
                    val root = json.parseToJsonElement(responseBody).jsonObject
                    val accessToken = root["access_token"]?.jsonPrimitive?.content
                    val userObj = root["user"]?.jsonObject ?: root

                    val userId = userObj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                    val user = User(id = userId, displayName = displayName)

                    if (accessToken != null) {
                        SupabaseClient.authToken = accessToken
                    }

                    // Try to ensure public profile row exists
                    try {
                        val profilePayload = """{"id":"$userId","display_name":"$displayName"}"""
                        SupabaseClient.post("/rest/v1/profiles", profilePayload)
                    } catch (_: Exception) {}

                    _currentUser.value = user

                    prefs?.edit()?.apply {
                        putString("user_id", userId)
                        putString("display_name", displayName)
                        putString("auth_token", accessToken)
                        apply()
                    }

                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to parse signup response: ${e.message}"))
                }
            },
            onFailure = { error ->
                val friendlyMessage = extractErrorMessage(error.message)
                Result.failure(Exception(friendlyMessage))
            }
        )
    }

    override suspend fun signOut() {
        SupabaseClient.authToken = null
        prefs?.edit()?.clear()?.apply()
        _currentUser.value = null
    }

    private fun extractErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "Network error or unexpected response"
        try {
            val jsonIndex = raw.indexOf('{')
            if (jsonIndex >= 0) {
                val jsonPart = raw.substring(jsonIndex)
                val obj = json.parseToJsonElement(jsonPart).jsonObject
                obj["msg"]?.jsonPrimitive?.content?.let { return it }
                obj["error_description"]?.jsonPrimitive?.content?.let { return it }
                obj["message"]?.jsonPrimitive?.content?.let { return it }
            }
        } catch (_: Exception) {}
        return raw.replace(Regex("""^HTTP \d+:\s*"""), "").ifBlank { "Authentication request failed" }
    }
}
