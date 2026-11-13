package com.poupa.vinylmusicplayer.util;

import android.view.View;
import android.os.Build;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Protects playback controls while leaving the existing top-status-bar views in charge. */
public final class PlaybackInsets {
    private PlaybackInsets() {}

    public static void apply(View container) {
        // Android 14 still fits the navigation bar through the window decor.
        // Android 15+ enforces edge-to-edge for our target SDK.
        if (Build.VERSION.SDK_INT < 35) return;

        final int left = container.getPaddingLeft();
        final int top = container.getPaddingTop();
        final int right = container.getPaddingRight();
        final int bottom = container.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(container, (view, insets) -> {
            final Insets safeArea = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + safeArea.left, top, right + safeArea.right, bottom + safeArea.bottom);
            // StatusBarView/StatusBarMarginFrameLayout still need the top inset.
            return insets;
        });
        ViewCompat.requestApplyInsets(container);
    }
}
