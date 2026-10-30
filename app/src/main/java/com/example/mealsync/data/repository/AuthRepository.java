package com.example.mealsync.data.repository;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

/**
 * The only class that talks to Firebase Authentication.
 * It never stores an Activity, so it cannot leak one.
 */
public class AuthRepository {

    public interface PhoneVerificationCallback {
        void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token);

        void onAutoVerified(PhoneAuthCredential credential);

        void onFailed(Exception error);
    }

    public interface SignInCallback {
        void onSuccess(FirebaseUser user);

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

    /** Asks Firebase to send a code. Pass a resend token to request a new code. */
    public void startPhoneVerification(Activity activity, String e164Phone,
                                       @Nullable PhoneAuthProvider.ForceResendingToken resendToken,
                                       PhoneVerificationCallback callback) {
        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(e164Phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        callback.onAutoVerified(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        callback.onFailed(e);
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        callback.onCodeSent(verificationId, token);
                    }
                });
        if (resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }
        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    public PhoneAuthCredential buildCredential(String verificationId, String code) {
        return PhoneAuthProvider.getCredential(verificationId, code);
    }

    public void signInWithCredential(AuthCredential credential, SignInCallback callback) {
        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null
                    && task.getResult().getUser() != null) {
                callback.onSuccess(task.getResult().getUser());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }
}