package com.example.mealsync.data.repository;

import androidx.annotation.Nullable;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * The only class that talks to Firebase Authentication.
 * It never stores an Activity, so it cannot leak one.
 */
public class AuthRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(Exception error);
    }

    public interface SignInCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception error);
    }

    public interface EmailSignInCallback {
        void onSuccess(FirebaseUser user);
        void onUnverifiedEmail(String email);
        void onFailure(Exception error);
    }

    private final FirebaseAuth auth;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void signOut() {
        auth.signOut();
    }

    public void sendPasswordResetEmail(String email, ActionCallback callback) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void registerWithEmail(String email, String password, ActionCallback callback) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                FirebaseUser user = task.getResult().getUser();
                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                    auth.signOut();
                    if (verifyTask.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure(verifyTask.getException());
                    }
                });
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void signInWithEmail(String email, String password, EmailSignInCallback callback) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                FirebaseUser user = task.getResult().getUser();
                user.reload().addOnCompleteListener(reloadTask -> {
                    if (user.isEmailVerified()) {
                        user.getIdToken(true).addOnCompleteListener(tokenTask -> callback.onSuccess(user));
                    } else {
                        String userEmail = user.getEmail() != null ? user.getEmail() : email;
                        auth.signOut();
                        callback.onUnverifiedEmail(userEmail);
                    }
                });
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    public void signInWithCredential(AuthCredential credential, SignInCallback callback) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                FirebaseUser user = task.getResult().getUser();
                user.getIdToken(true).addOnCompleteListener(tokenTask -> callback.onSuccess(user));
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}
