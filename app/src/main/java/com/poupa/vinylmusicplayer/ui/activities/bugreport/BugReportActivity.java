package com.poupa.vinylmusicplayer.ui.activities.bugreport;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.TintHelper;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.databinding.ActivityBugReportBinding;
import com.poupa.vinylmusicplayer.ui.activities.base.AbsThemeActivity;
import com.poupa.vinylmusicplayer.ui.activities.bugreport.model.DeviceInfo;
import com.poupa.vinylmusicplayer.util.SafeToast;

public class BugReportActivity extends AbsThemeActivity {
    private Toolbar toolbar;

    private TextView textDeviceInfo;

    private FloatingActionButton sendFab;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActivityBugReportBinding binding = ActivityBugReportBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        toolbar = binding.toolbar;
        sendFab = binding.buttonSend;

        textDeviceInfo = binding.bugReportCardDeviceInfo.airTextDeviceInfo;

        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        initViews();

        if (TextUtils.isEmpty(getTitle())) {
            setTitle(R.string.report_an_issue);
        }

        final DeviceInfo deviceInfo = new DeviceInfo(this);
        final String extraInfo = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        textDeviceInfo.setText(deviceInfo + (extraInfo != null ? ("\n\n" + extraInfo) : ""));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        final int accentColor = ThemeStore.accentColor(this);
        final int primaryColor = ThemeStore.primaryColor(this);
        toolbar.setBackgroundColor(primaryColor);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textDeviceInfo.setOnClickListener(v -> copyDeviceInfoToClipBoard());

        TintHelper.setTintAuto(sendFab, accentColor, true);
        sendFab.setOnClickListener(v -> reportIssue());
    }

    private void reportIssue() {
        copyDeviceInfoToClipBoard();

        // Needle has no public issue tracker configured yet. Let the user choose
        // where to send the report rather than filing fork bugs against Vinyl.
        final Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " + getString(R.string.report_an_issue))
                .putExtra(Intent.EXTRA_TEXT, textDeviceInfo.getText().toString());
        startActivity(Intent.createChooser(intent, getString(R.string.report_an_issue)));
    }

    private void copyDeviceInfoToClipBoard() {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(getString(R.string.device_info), textDeviceInfo.getText());
        clipboard.setPrimaryClip(clip);

        SafeToast.show(this, R.string.copied_device_info_to_clipboard);
    }
}
