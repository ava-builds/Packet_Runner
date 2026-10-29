package com.avabuilds.packetrunner.data

/** Checks what the player typed before anything is sent to Firebase. Each function returns a message, or null when the text is fine. */
object AuthValidation {
    const val MIN_PASSWORD = 6
    const val NICKNAME_MIN = 2
    const val NICKNAME_MAX = 16

    private val emailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun email(text: String): String? = when {
        text.isBlank() -> "Type your email."
        !emailShape.matches(text.trim()) -> "That doesn't look like an email address."
        else -> null
    }

    fun password(text: String): String? =
        if (text.length < MIN_PASSWORD) "The password needs at least $MIN_PASSWORD characters." else null

    fun nickname(text: String): String? {
        val n = text.trim()
        return when {
            n.length < NICKNAME_MIN -> "Your nickname needs at least $NICKNAME_MIN letters."
            n.length > NICKNAME_MAX -> "Your nickname can have at most $NICKNAME_MAX letters."
            else -> null
        }
    }
}
