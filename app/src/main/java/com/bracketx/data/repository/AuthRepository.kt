package com.bracketx.data.repository

import com.bracketx.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface AuthRepository {
    val currentUser: StateFlow<User?>
    suspend fun signIn(email: String, password: String):Result<User>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun signOut()
}

class InMemoryAuthRepository : AuthRepository {
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "usr_host_swarnendu",
            displayName = "Swarnendu",
            createdAt = "2026-09-15T12:00:00Z"
        )
    )
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<User> {
        val user = User(
            id = UUID.randomUUID().toString(),
            displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        val user = User(
            id = UUID.randomUUID().toString(),
            displayName = displayName.ifBlank { email.substringBefore("@") }
        )
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut() {
        _currentUser.value = null
    }
}
