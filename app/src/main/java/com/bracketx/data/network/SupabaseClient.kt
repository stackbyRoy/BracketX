package com.bracketx.data.network

import com.stackbyroy.bracketx.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SupabaseClient {

    val url: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var authToken: String? = null

    private fun newRequestBuilder(endpoint: String): Request.Builder {
        val builder = Request.Builder()
            .url("$url$endpoint")
            .header("apikey", anonKey)

        val token = authToken ?: anonKey
        builder.header("Authorization", "Bearer $token")
        return builder
    }

    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$url/auth/v1/health")
                .header("apikey", anonKey)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Health check failed: HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun get(path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder(path).get().build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun post(path: String, jsonBody: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder(path)
                .header("Prefer", "return=representation")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun patch(path: String, jsonBody: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder(path)
                .header("Prefer", "return=representation")
                .patch(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rpc(functionName: String, jsonBody: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = newRequestBuilder("/rest/v1/rpc/$functionName")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                Result.success(body)
            } else {
                Result.failure(Exception("RPC error ${response.code}: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
