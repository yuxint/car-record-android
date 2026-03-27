package com.tx.carrecord.core.common

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>

    data class Failure(val error: RepositoryError) : RepositoryResult<Nothing>
}

sealed interface RepositoryError {
    val message: String

    data class RuleViolation(
        val code: String,
        override val message: String,
    ) : RepositoryError

    data class NotFound(
        val code: String,
        override val message: String,
    ) : RepositoryError

    data class ConstraintConflict(
        val code: String,
        override val message: String,
    ) : RepositoryError

    data class StorageFailure(
        val code: String,
        override val message: String,
        val cause: Throwable? = null,
    ) : RepositoryError
}
