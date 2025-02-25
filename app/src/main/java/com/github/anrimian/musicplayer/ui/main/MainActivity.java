package com.github.anrimian.musicplayer.ui.main;

import static com.github.anrimian.musicplayer.Constants.Arguments.OPEN_PLAYER_PANEL_ARG;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.data.utils.Permissions;
import com.github.anrimian.musicplayer.databinding.DialogErrorReportBinding;
import com.github.anrimian.musicplayer.di.Components;
import com.github.anrimian.musicplayer.di.app.AppComponent;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.repositories.LoggerRepository;
import com.github.anrimian.musicplayer.ui.main.setup.SetupFragment;
import com.github.anrimian.musicplayer.ui.player_screen.PlayerFragment;
import com.github.anrimian.musicplayer.ui.player_screen.PlayerFragmentKt;
import com.github.anrimian.musicplayer.ui.utils.AndroidUtils;
import com.github.anrimian.musicplayer.ui.utils.ViewUtils;
import com.github.anrimian.musicplayer.ui.utils.fragments.BackButtonListener;
import com.github.anrimian.musicplayer.ui.utils.fragments.FragmentUtilsKt;
import com.github.anrimian.musicplayer.utils.logger.AppLogger;
import com.github.anrimian.musicplayer.utils.logger.FileLog;

public class MainActivity extends AppCompatActivity {

    public static void showInFolders(FragmentActivity activity, Composition composition) {
        Fragment currentFragment = activity.getSupportFragmentManager()
                .findFragmentById(R.id.main_activity_container);
        if (currentFragment instanceof PlayerFragment) {
            ((PlayerFragment) currentFragment).locateCompositionInFolders(composition);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Components.getAppComponent().themeController().applyCurrentTheme(this);
        super.onCreate(savedInstanceState);

        //trick to fix internal id issue in ViewPager2.
        //Delete after this issue will be solved: https://issuetracker.google.com/issues/185820237
        //Solution is taken from https://stackoverflow.com/a/59989710/5541688
        //counter reduced from 1000 to 100, return back if fix won't help
        for (int i = 0; i < 99; i++) {
            ViewCompat.generateViewId();
        }

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            LoggerRepository loggerRepository = Components.getAppComponent().loggerRepository();
            if ((loggerRepository.wasFatalError() && loggerRepository.isReportDialogOnStartEnabled())
                    || loggerRepository.wasCriticalFatalError()) {
                FragmentUtilsKt.safeShow(new ErrorReportDialogFragment(), getSupportFragmentManager(), null);
                if (loggerRepository.wasCriticalFatalError()) {
                    return;
                }
            }
            startScreens();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(Components.getAppComponent().localeController().dispatchAttachBaseContext(base));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_activity_container);
        if (fragment instanceof PlayerFragment) {
            PlayerFragment playerFragment = (PlayerFragment) fragment;
            if (getOpenPlayerPanelArg(intent)) {
                playerFragment.openPlayerPanel();//non-smooth update, why...
            }
            String playlistUri = getPlaylistArg(intent);
            if (playlistUri != null) {
                playerFragment.openImportPlaylistScreen(playlistUri);
            }
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_activity_container);
        if (fragment instanceof BackButtonListener && ((BackButtonListener) fragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        AndroidUtils.hideKeyboard(getWindow().getDecorView());
    }

    private void startScreens() {
        if (Permissions.hasFilePermission(this)) {
            goToMainScreen();
        } else {
            goToSetupScreen();
        }
    }

    private void goToSetupScreen() {
        startFragment(new SetupFragment());
    }

    private void goToMainScreen() {
        Intent intent = getIntent();

        boolean openPlayQueue = getOpenPlayerPanelArg(intent);

        String playlistUri = null;
        if (!AndroidUtils.isLaunchedFromHistory(this)) {
            playlistUri = getPlaylistArg(intent);
        }
        startFragment(PlayerFragmentKt.newPlayerFragment(openPlayQueue, playlistUri));
    }

    private void startFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existFragment = fragmentManager.findFragmentById(R.id.main_activity_container);
        if (existFragment == null || existFragment.getClass() != fragment.getClass()) {
            fragmentManager.beginTransaction()
                    .replace(R.id.main_activity_container, fragment)
                    .commit();
        }
    }

    private boolean getOpenPlayerPanelArg(Intent intent) {
        boolean openPlayerPanel = intent.getBooleanExtra(OPEN_PLAYER_PANEL_ARG, false);
        getIntent().removeExtra(OPEN_PLAYER_PANEL_ARG);
        return openPlayerPanel;
    }

    @Nullable
    private String getPlaylistArg(Intent intent) {
        String type = intent.getType();
        if ("audio/x-mpegurl".equals(type) || "audio/mpegurl".equals(type)) {
            return intent.getData().toString();
        }
        return null;
    }

    public static class ErrorReportDialogFragment extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LoggerRepository loggerRepository = Components.getAppComponent().loggerRepository();
            boolean isCritical = loggerRepository.wasCriticalFatalError();

            DialogErrorReportBinding binding = DialogErrorReportBinding.inflate(getLayoutInflater());
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.error_report)
                    .setMessage(isCritical? R.string.critical_error_report_message: R.string.error_report_message)
                    .setView(binding.getRoot())
                    .show();

            binding.cbShowReportDialogOnStart.setVisibility(isCritical? View.GONE: View.VISIBLE);
            binding.cbShowReportDialogOnStart.setChecked(loggerRepository.isReportDialogOnStartEnabled());
            ViewUtils.onCheckChanged(binding.cbShowReportDialogOnStart, loggerRepository::showReportDialogOnStart);

            FileLog fileLog = Components.getAppComponent().fileLog();

            binding.btnDelete.setOnClickListener(v -> {
                fileLog.deleteLogFile();
                dismissAllowingStateLoss();
                onReportDialogClosed();
            });

            AppLogger appLogger = Components.getAppComponent().appLogger();

            binding.btnView.setOnClickListener(v -> appLogger.startViewLogScreen(requireActivity()));
            binding.btnSend.setOnClickListener(v -> {
                appLogger.startSendLogScreen(requireActivity());
                dismissAllowingStateLoss();
                onReportDialogClosed();
            });

            binding.btnClose.setOnClickListener(v -> {
                dismissAllowingStateLoss();
                onReportDialogClosed();
            });

            return dialog;
        }

        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            super.onCancel(dialog);
            onReportDialogClosed();
        }

        private void onReportDialogClosed() {
            AppComponent appComponent = Components.getAppComponent();
            LoggerRepository loggerRepository = appComponent.loggerRepository();
            boolean isCritical = loggerRepository.wasCriticalFatalError();
            loggerRepository.clearErrorFlags();

            if (isCritical) {
                ((MainActivity) getActivity()).startScreens();
                if (Permissions.hasFilePermission(requireContext())) {
                    appComponent.widgetUpdater().start();
                    appComponent.mediaScannerRepository().runStorageObserver();
                }
            }
        }

    }

}
