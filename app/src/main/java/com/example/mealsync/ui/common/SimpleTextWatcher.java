package com.example.mealsync.ui.common;

import android.text.TextWatcher;

/** TextWatcher where only afterTextChanged has to be written. */
public abstract class SimpleTextWatcher implements TextWatcher {

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}