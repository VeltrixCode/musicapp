package com.example.musicapp.model

sealed class AppError(
    open val message: String,
    open val actionLabel: String? = null,
    open val onAction: (() -> Unit)? = null,
) {
    data class NoNetwork(
        override val onAction: (() -> Unit)? = null
    ) : AppError(
        message = "Отсутствует подключение к сети",
        actionLabel = if (onAction != null) "Повторить" else null,
        onAction = onAction
    )

    data class TokenExpired(
        val source: TrackSource,
        override val onAction: (() -> Unit)? = null
    ) : AppError(
        message = "Срок действия токена ${source.name} истёк",
        actionLabel = if (onAction != null) "Войти снова" else null,
        onAction = onAction
    )

    data class AccessDenied(
        val details: String = "Доступ к аудиофайлу ограничен или запрещён"
    ) : AppError(
        message = details,
        actionLabel = null,
        onAction = null
    )

    data class QuotaExceeded(
        override val onAction: (() -> Unit)? = null
    ) : AppError(
        message = "Превышена квота запросов к сервису",
        actionLabel = if (onAction != null) "Повторить" else null,
        onAction = onAction
    )

    data class UnsupportedFormat(
        val formatName: String
    ) : AppError(
        message = "Формат $formatName не поддерживается встроенным движком JavaFX Media",
        actionLabel = null,
        onAction = null
    )

    data class Generic(
        override val message: String,
        override val onAction: (() -> Unit)? = null
    ) : AppError(
        message = message,
        actionLabel = if (onAction != null) "Повторить" else null,
        onAction = onAction
    )
}
