package com.example.mealsync.ui.common;

import android.view.View;
import android.widget.ScrollView;

public final class StickyHeaderBehavior {

    private StickyHeaderBehavior() {
    }

    public static void attach(ScrollView scrollView, View header, int backgroundResource) {
        header.setBackgroundResource(backgroundResource);
        float density = header.getResources().getDisplayMetrics().density;
        header.setElevation(16f * density);
        header.setTranslationZ(16f * density);
        scrollView.setOnScrollChangeListener((View view, int scrollX, int scrollY,
                                              int oldScrollX, int oldScrollY) -> {
            header.setTranslationY(scrollY);
            header.invalidate();
        });
    }
}
