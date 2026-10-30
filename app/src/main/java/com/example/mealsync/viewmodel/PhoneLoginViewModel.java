package com.example.mealsync.viewmodel;

import android.app.Activity;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mealsync.R;
import com.example.mealsync.data.repository.AuthRepository;
import com.example.mealsync.util.PhoneFormatter;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class PhoneLoginViewModel extends ViewModel {

    public enum Stage {
        ENTER_PHONE,
        ENTER_CODE,
        SIGNED_IN
    }

    public enum ErrorTarget {
        PHONE,
        CODE,
        GENERAL
    }

    public static class UiError {
        @StringRes public final int messageRes;
        @Nullable public final String detail;
        @NonNull public final ErrorTarget target;

        public UiError(@StringRes int messageRes) {
            this(messageRes, null, ErrorTarget.GENERAL);
        }

        public UiError(@StringRes int messageRes, @NonNull ErrorTarget target) {
            this(messageRes, null, target);
        }

        public UiError(@StringRes int messageRes, @Nullable String detail, @NonNull ErrorTarget target) {
            this.messageRes = messageRes;
            this.detail = detail;
            this.target = target;
        }
    }

    private final AuthRepository repository;
    private final MutableLiveData<Stage> stage = new MutableLiveData<>(Stage.ENTER_PHONE);
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<UiError> error = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> resendSeconds = new MutableLiveData<>(0);

    private String phoneE164;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private CountDownTimer countDownTimer;

    public PhoneLoginViewModel() {
        this(new AuthRepository());
    }

    public PhoneLoginViewModel(AuthRepository repository) {
        this.repository = repository;
    }

    public LiveData<Stage> getStage() {
        return stage;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public LiveData<UiError> getError() {
        return error;
    }

    public LiveData<Integer> getResendSeconds() {
        return resendSeconds;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public void consumeError() {
        error.setValue(null);
    }

    public void backToPhone() {
        cancelTimer();
        verificationId = null;
        resendToken = null;
        stage.setValue(Stage.ENTER_PHONE);
    }

    public void submitPhone(Activity activity, String rawPhone) {
        String e164 = PhoneFormatter.toE164(rawPhone);
        if (e164 == null) {
            error.setValue(new UiError(R.string.err_invalid_phone, ErrorTarget.PHONE));
            return;
        }
        this.phoneE164 = e164;
        sendVerificationCode(activity, false);
    }

    public void resendCode(Activity activity) {
        if (phoneE164 == null) {
            error.setValue(new UiError(R.string.err_no_verification, ErrorTarget.GENERAL));
            return;
        }
        sendVerificationCode(activity, true);
    }

    private void sendVerificationCode(Activity activity, boolean isResend) {
        if (repository == null) {
            return;
        }
        busy.setValue(true);
        repository.startPhoneVerification(activity, phoneE164, isResend ? resendToken : null,
                new AuthRepository.PhoneVerificationCallback() {
                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        PhoneLoginViewModel.this.verificationId = verificationId;
                        PhoneLoginViewModel.this.resendToken = token;
                        busy.setValue(false);
                        stage.setValue(Stage.ENTER_CODE);
                        startResendTimer();
                    }

                    @Override
                    public void onAutoVerified(PhoneAuthCredential credential) {
                        signInWithCredential(credential);
                    }

                    @Override
                    public void onFailed(Exception e) {
                        busy.setValue(false);
                        error.setValue(mapError(e));
                    }
                });
    }

    public void submitCode(String code) {
        if (code == null || code.trim().length() < 6) {
            error.setValue(new UiError(R.string.err_code_incomplete, ErrorTarget.CODE));
            return;
        }
        if (verificationId == null) {
            error.setValue(new UiError(R.string.err_no_verification, ErrorTarget.GENERAL));
            return;
        }
        if (repository == null) {
            return;
        }
        busy.setValue(true);
        PhoneAuthCredential credential = repository.buildCredential(verificationId, code.trim());
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        if (repository == null) {
            return;
        }
        repository.signInWithCredential(credential, new AuthRepository.SignInCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                busy.setValue(false);
                stage.setValue(Stage.SIGNED_IN);
            }

            @Override
            public void onFailure(Exception e) {
                busy.setValue(false);
                error.setValue(mapError(e));
            }
        });
    }

    private void startResendTimer() {
        cancelTimer();
        resendSeconds.setValue(60);
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendSeconds.setValue((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                resendSeconds.setValue(0);
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        resendSeconds.setValue(0);
    }

    private UiError mapError(Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return new UiError(R.string.err_network, ErrorTarget.GENERAL);
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return new UiError(R.string.err_too_many, ErrorTarget.GENERAL);
        }
        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            switch (errorCode) {
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return new UiError(R.string.err_phone_auth_disabled, ErrorTarget.GENERAL);
                case "ERROR_APP_NOT_AUTHORIZED":
                    return new UiError(R.string.err_app_not_authorized, ErrorTarget.GENERAL);
                case "ERROR_INVALID_PHONE_NUMBER":
                    return new UiError(R.string.err_invalid_phone, ErrorTarget.PHONE);
                case "ERROR_INVALID_VERIFICATION_CODE":
                    return new UiError(R.string.err_wrong_code, ErrorTarget.CODE);
                case "ERROR_SESSION_EXPIRED":
                    return new UiError(R.string.err_code_expired, ErrorTarget.CODE);
                case "ERROR_QUOTA_EXCEEDED":
                    return new UiError(R.string.err_quota_exceeded, ErrorTarget.GENERAL);
                case "ERROR_INTERNAL_ERROR":
                    return new UiError(R.string.err_internal_error, ErrorTarget.GENERAL);
            }
        }
        String detail = e != null ? (e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getMessage()) : null;
        return new UiError(R.string.err_generic, detail, ErrorTarget.GENERAL);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cancelTimer();
    }
}
