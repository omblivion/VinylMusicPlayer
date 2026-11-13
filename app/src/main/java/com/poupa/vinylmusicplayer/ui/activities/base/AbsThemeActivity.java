package com.poupa.vinylmusicplayer.ui.activities.base;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.activity.OnBackPressedCallback;

import com.kabouzeid.appthemehelper.ATH;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.ATHToolbarActivity;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialDialogsUtil;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.util.OopsHandler;
import com.poupa.vinylmusicplayer.util.PreferenceUtil;
import com.poupa.vinylmusicplayer.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */

public abstract class AbsThemeActivity extends ATHToolbarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new OopsHandler(this));

        setTheme(PreferenceUtil.getInstance().getGeneralTheme());
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!handleBackPressed()) {
                    setEnabled(false);
                    try {
                        getOnBackPressedDispatcher().onBackPressed();
                    } finally {
                        setEnabled(true);
                    }
                }
            }
        });
        MaterialDialogsUtil.updateMaterialDialogsThemeSingleton(this);
    }

    protected boolean handleBackPressed() {
        return false;
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        super.onDestroy();
    }

    protected void setDrawUnderStatusbar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            Util.setAllowDrawUnderStatusBar(getWindow());
        else Util.setStatusBarTranslucent(getWindow());
    }

    public void setStatusbarColor(int color) {
        if (Build.VERSION.SDK_INT >= 35) {
            // Edge-to-edge ignores Window.setStatusBarColor. Paint the backdrop
            // that already reserves the status-bar inset in our layouts.
            final View statusBar = getWindow().getDecorView().getRootView().findViewById(R.id.status_bar);
            if (statusBar != null) {
                statusBar.setBackgroundColor(color);
            }
        } else {
            getWindow().setStatusBarColor(color);
        }
        setLightStatusbarAuto(color);
    }

    public void setStatusbarColorAuto() {
        // we don't want to use statusbar color because we are doing the color darkening on our own to support KitKat
        setStatusbarColor(ThemeStore.primaryColor(this));
    }

    public void setTaskDescriptionColor(@ColorInt int color) {
        ATH.setTaskDescriptionColor(this, color);
    }

    public void setTaskDescriptionColorAuto() {
        setTaskDescriptionColor(ThemeStore.primaryColor(this));
    }

    public void setNavigationbarColor(int color) {
        if (ThemeStore.coloredNavigationBar(this)) {
            ATH.setNavigationbarColor(this, color);
        } else {
            ATH.setNavigationbarColor(this, Color.BLACK);
        }
    }

    public void setNavigationbarColorAuto() {
        setNavigationbarColor(ThemeStore.navigationBarColor(this));
    }

    public void setLightStatusbar(boolean enabled) {
        ATH.setLightStatusbar(this, enabled);
    }

    public void setLightStatusbarAuto(int bgColor) {
        setLightStatusbar(ColorUtil.isColorLight(bgColor));
    }
}
