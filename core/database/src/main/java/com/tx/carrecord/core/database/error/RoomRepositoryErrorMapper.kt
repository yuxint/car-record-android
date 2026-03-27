package com.tx.carrecord.core.database.error

import android.database.sqlite.SQLiteConstraintException
import com.tx.carrecord.core.common.RepositoryError

object RoomRepositoryErrorMapper {
    fun map(throwable: Throwable): RepositoryError = when (throwable) {
        is SQLiteConstraintException -> RepositoryError.ConstraintConflict(
            code = "DB_CONSTRAINT_CONFLICT",
            message = "数据写入失败：违反唯一性或外键约束。",
        )

        else -> RepositoryError.StorageFailure(
            code = "DB_STORAGE_FAILURE",
            message = "数据写入失败：数据库异常。",
            cause = throwable,
        )
    }
}
