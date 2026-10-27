package com.avabuilds.packetrunner.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest

/** [AuthRepository] on Firebase Authentication (email and password). The nickname is stored as the user's display name. */
class FirebaseAuthRepository : AuthRepository {
    private val auth get() = FirebaseAuth.getInstance()

    override val currentUser: UserProfile? get() = auth.currentUser?.toProfile()

    override fun signUp(email: String, password: String, nickname: String, onDone: (AuthOutcome) -> Unit) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val user = result.user!!
                user.updateProfile(userProfileChangeRequest { displayName = nickname.trim() })
                    .addOnCompleteListener { onDone(AuthOutcome.Success(user.toProfile())) }
            }
            .addOnFailureListener { onDone(AuthOutcome.Failure(friendlyAuthError(it.code()))) }
    }

    override fun signIn(email: String, password: String, onDone: (AuthOutcome) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { onDone(AuthOutcome.Success(it.user?.toProfile())) }
            .addOnFailureListener { onDone(AuthOutcome.Failure(friendlyAuthError(it.code()))) }
    }

    override fun signOut() = auth.signOut()

    override fun updateNickname(nickname: String, onDone: (AuthOutcome) -> Unit) {
        val user = auth.currentUser ?: return onDone(AuthOutcome.Failure("You are not logged in."))
        user.updateProfile(userProfileChangeRequest { displayName = nickname.trim() })
            .addOnSuccessListener { onDone(AuthOutcome.Success(user.toProfile())) }
            .addOnFailureListener { onDone(AuthOutcome.Failure(friendlyAuthError(it.code()))) }
    }

    override fun deleteAccount(password: String, onDone: (AuthOutcome) -> Unit) {
        val user = auth.currentUser ?: return onDone(AuthOutcome.Failure("You are not logged in."))
        val credential = EmailAuthProvider.getCredential(user.email.orEmpty(), password)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener { onDone(AuthOutcome.Success(null)) }
                    .addOnFailureListener { onDone(AuthOutcome.Failure(friendlyAuthError(it.code()))) }
            }
            .addOnFailureListener { onDone(AuthOutcome.Failure(friendlyAuthError(it.code()))) }
    }

    companion object {
        /** True when google-services.json was added, so Firebase started with the app. */
        fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
    }
}

private fun FirebaseUser.toProfile() = UserProfile(
    uid = uid,
    email = email.orEmpty(),
    nickname = displayName?.takeIf { it.isNotBlank() } ?: email.orEmpty().substringBefore('@'),
)

private fun Exception.code(): String = when (this) {
    is FirebaseAuthException -> errorCode
    is FirebaseNetworkException -> "NETWORK"
    else -> "UNKNOWN"
}
