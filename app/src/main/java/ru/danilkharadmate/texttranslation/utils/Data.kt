package ru.danilkharadmate.texttranslation.utils

sealed class Data<out T>{
    object Loading: Data<Nothing>()
    data class Error<T>(val message: String,val localData: T? = null): Data<T>()
    data class Ok<T>(val data: T, val remote: Boolean = true): Data<T>()
}
