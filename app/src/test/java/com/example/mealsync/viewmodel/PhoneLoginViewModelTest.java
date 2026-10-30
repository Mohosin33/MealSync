package com.example.mealsync.viewmodel;

import com.example.mealsync.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PhoneLoginViewModelTest {

    private PhoneLoginViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new PhoneLoginViewModel(null);
    }

    private PhoneLoginViewModel.UiError invokeMapError(Exception e) throws Exception {
        Method method = PhoneLoginViewModel.class.getDeclaredMethod("mapError", Exception.class);
        method.setAccessible(true);
        return (PhoneLoginViewModel.UiError) method.invoke(viewModel, e);
    }

    @Test
    public void testNetworkError() throws Exception {
        FirebaseNetworkException e = new FirebaseNetworkException("No network");
        PhoneLoginViewModel.UiError error = invokeMapError(e);
        assertNotNull(error);
        assertEquals(R.string.err_network, error.messageRes);
    }

    @Test
    public void testTooManyRequestsError() throws Exception {
        FirebaseTooManyRequestsException e = new FirebaseTooManyRequestsException("Too many");
        PhoneLoginViewModel.UiError error = invokeMapError(e);
        assertNotNull(error);
        assertEquals(R.string.err_too_many, error.messageRes);
    }

    @Test
    public void testPhoneAuthDisabledError() throws Exception {
        FirebaseAuthException e = new FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "Disabled");
        PhoneLoginViewModel.UiError error = invokeMapError(e);
        assertNotNull(error);
        assertEquals(R.string.err_phone_auth_disabled, error.messageRes);
    }

    @Test
    public void testAppNotAuthorizedError() throws Exception {
        FirebaseAuthException e = new FirebaseAuthException("ERROR_APP_NOT_AUTHORIZED", "Not authorized");
        PhoneLoginViewModel.UiError error = invokeMapError(e);
        assertNotNull(error);
        assertEquals(R.string.err_app_not_authorized, error.messageRes);
    }

    @Test
    public void testInvalidPhoneError() throws Exception {
        FirebaseAuthException e = new FirebaseAuthException("ERROR_INVALID_PHONE_NUMBER", "Invalid");
        PhoneLoginViewModel.UiError error = invokeMapError(e);
        assertNotNull(error);
        assertEquals(R.string.err_invalid_phone, error.messageRes);
        assertEquals(PhoneLoginViewModel.ErrorTarget.PHONE, error.target);
    }

    @Test
    public void testInternalError() throws Exception {
        FirebaseAuthException e = new FirebaseAuthException("ERROR_INTERNAL_ERROR", "An internal error has occurred.");
        PhoneLoginViewModel.UiError error = invokeMapError(e);
        assertNotNull(error);
        assertEquals(R.string.err_internal_error, error.messageRes);
    }
}
