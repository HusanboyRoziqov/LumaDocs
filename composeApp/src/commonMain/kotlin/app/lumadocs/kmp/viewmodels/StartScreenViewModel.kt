package app.lumadocs.kmp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lumadocs.kmp.Authenticator
import app.lumadocs.kmp.CurrentUserState
import app.lumadocs.kmp.data.FirebaseUser
import app.lumadocs.kmp.data.Response
import app.lumadocs.kmp.utils.ErrorMessages
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StartScreenViewModel(
    private val authenticator: Authenticator,
) : ViewModel() {

    private val _uiState = MutableStateFlow<State>(State())
    val uiState = _uiState.asStateFlow()

    private val navigateToHome = MutableSharedFlow<FirebaseUser>()
    val navigateToHomeFlow = navigateToHome.asSharedFlow()

    fun checkCurrentUser() {
        viewModelScope.launch {
            val currentUser = authenticator.getCurrentUser()
            if (currentUser != null && !currentUser.userEmail.isNullOrEmpty()) {
                CurrentUserState.set(currentUser)
                _uiState.update { it.copy(data = currentUser) }
                navigateToHome.emit(currentUser)
            } else {
                onGoogleSignInClicked()
            }
        }
    }

    fun checkUser() = authenticator.getCurrentUser()

    fun onGoogleSignInClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            when (val response = authenticator.login()) {
                is Response.Failure -> {
                    val message = ErrorMessages.forAuthCode(response.error)
                    _uiState.update { it.copy(loading = false, error = message) }
                }

                is Response.Success<*> -> {
                    val user = response.data as FirebaseUser
                    CurrentUserState.set(user)
                    _uiState.update { it.copy(loading = false, data = user) }
                    navigateToHome.emit(user)
                }

                is Response.Loading -> {
                    _uiState.update { it.copy(loading = response.loading) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class State(
    val loading: Boolean = false,
    val data: FirebaseUser = FirebaseUser(),
    val error: String? = null,
)
