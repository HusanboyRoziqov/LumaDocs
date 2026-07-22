package app.lumadocs.kmp.data

sealed class Response<out T> {

    data class Success<out T>(val data: T) : Response<T>()

    data class Failure(val error: String) : Response<Nothing>()

    data class Loading(val loading: Boolean = false) : Response<Nothing>()
}
