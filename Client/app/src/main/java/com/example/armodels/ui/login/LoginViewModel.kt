package com.example.armodels.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.armodels.repository.AuthRepository
import com.example.armodels.utils.ErrorMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val navigateToLibrary: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, error = null)
    }

    fun login() = authenticate { u, p -> authRepository.login(u, p) }

    fun register() = authenticate { u, p -> authRepository.register(u, p) }

    private fun authenticate(action: suspend (String, String) -> Result<String>) {
        val state = _uiState.value
        if (!validate(state.username, state.password)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = action(state.username, state.password)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false, navigateToLibrary = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ErrorMapper.map(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToLibrary = false)
    }

    private fun validate(username: String, password: String): Boolean {
        val usernameRegex = Regex("^[A-Za-z0-9_-]{5,20}$")
        val passwordRegex = Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-=]{8,40}$")
        return when {
            !usernameRegex.matches(username) -> {
                _uiState.value = _uiState.value.copy(
                    error = "Логин: 5–20 символов, только A-Z, 0-9, _ -"
                )
                false
            }
            !passwordRegex.matches(password) -> {
                _uiState.value = _uiState.value.copy(
                    error = "Пароль: 8–40 символов"
                )
                false
            }
            else -> true
        }
    }
}