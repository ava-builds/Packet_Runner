package com.avabuilds.packetrunner.data

/** The signed-in player. [nickname] is what the town calls them. */
data class UserProfile(val uid: String, val email: String, val nickname: String)

/** The result of an account action: it worked, or here is a message for the player. */
sealed interface AuthOutcome {
    data class Success(val user: UserProfile?) : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

/**
 * Everything the game needs from accounts. The screens only know this interface, so the
 * Firebase code stays in one place ([FirebaseAuthRepository]) and the rest can be tested without it.
 */
interface AuthRepository {
    val currentUser: UserProfile?
    fun signUp(email: String, password: String, nickname: String, onDone: (AuthOutcome) -> Unit)
    fun signIn(email: String, password: String, onDone: (AuthOutcome) -> Unit)
    fun signOut()
    fun updateNickname(nickname: String, onDone: (AuthOutcome) -> Unit)

    /**
     * Deleting an account needs a recent login, so the password is asked again. Once the password is
     * right, [beforeDelete] runs (to remove the player's saved data) and says through its callback
     * whether to go on; only then is the account deleted.
     */
    fun deleteAccount(
        password: String,
        beforeDelete: (uid: String, proceed: (Boolean) -> Unit) -> Unit,
        onDone: (AuthOutcome) -> Unit,
    )
}

/** Turns a Firebase error code into a sentence a player understands. */
fun friendlyAuthError(code: String?): String = when (code) {
    "ERROR_INVALID_EMAIL" -> "That doesn't look like an email address."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "There is already an account with this email. Try logging in."
    "ERROR_WEAK_PASSWORD" -> "That password is too weak. Use at least ${AuthValidation.MIN_PASSWORD} characters."
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> "The email or the password is wrong."
    "ERROR_USER_DISABLED" -> "This account has been switched off."
    "ERROR_TOO_MANY_REQUESTS" -> "Too many tries. Wait a little and try again."
    "ERROR_REQUIRES_RECENT_LOGIN" -> "For safety, please log in again and then retry."
    "NETWORK" -> "No internet connection. Check your network and try again."
    else -> "Something went wrong. Please try again."
}
