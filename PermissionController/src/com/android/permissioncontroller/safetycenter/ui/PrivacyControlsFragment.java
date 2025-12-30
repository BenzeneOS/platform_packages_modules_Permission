/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.safetycenter.ui;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.permissioncontroller.R;
import com.android.permissioncontroller.safetycenter.ui.model.PrivacyControlsViewModel;
import com.android.permissioncontroller.safetycenter.ui.model.PrivacyControlsViewModel.Pref;
import com.android.permissioncontroller.safetycenter.ui.model.PrivacyControlsViewModel.PrefState;
import com.android.permissioncontroller.safetycenter.ui.model.PrivacyControlsViewModelFactory;

import java.util.Map;

/** Fragment that shows several privacy toggle controls, alongside a link to location settings */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public final class PrivacyControlsFragment extends PreferenceFragmentCompat {

    /** Create a new instance of this fragment */
    public static PrivacyControlsFragment newInstance() {
        return new PrivacyControlsFragment();
    }

    private PrivacyControlsViewModel mViewModel;
    private String mRootKey;
    private boolean mPrefsSet;

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        mRootKey = rootKey;
        mPrefsSet = false;

        PrivacyControlsViewModelFactory factory =
                new PrivacyControlsViewModelFactory(getActivity().getApplication());
        mViewModel = new ViewModelProvider(this, factory).get(PrivacyControlsViewModel.class);
        mViewModel.getControlStateLiveData().observe(this, this::setPreferences);
    }

    private void setPreferences(Map<Pref, PrefState> prefStates) {
        // Delaying setting of preferences, in order to avoid disabled prefs being briefly visible
        if (!mPrefsSet) {
            setPreferencesFromResource(R.xml.privacy_controls, mRootKey);
            mPrefsSet = true;
        }

        setSwitchPreference(prefStates, Pref.MIC);
        setSwitchPreference(prefStates, Pref.CAMERA);
        setSwitchPreference(prefStates, Pref.CLIPBOARD);
        setSwitchPreference(prefStates, Pref.CLIPBOARD_AUTO_CLEAR);
        setSwitchPreference(prefStates, Pref.SHOW_PASSWORD);

        // Setup timeout preference
        setupTimeoutPreference();

        findPreference(Pref.LOCATION.getKey())
                .setOnPreferenceClickListener(
                        (v) -> {
                            mViewModel.handlePrefClick(this, Pref.LOCATION, null);
                            return true;
                        });
    }

    private void setSwitchPreference(Map<Pref, PrefState> prefStates, Pref prefType) {
        ClickableDisabledSwitchPreference preference = findPreference(prefType.getKey());
        preference.setupState(prefStates.get(prefType), prefType, mViewModel, this);
    }

    private static final String TIMEOUT_KEY = "clipboard_auto_clear_timeout";
    private static final String[] TIMEOUT_LABELS = {
        "30 seconds", "1 minute", "2 minutes", "5 minutes", "10 minutes",
        "30 minutes", "1 hour", "2 hours", "4 hours", "8 hours", "12 hours", "24 hours"
    };
    private static final long[] TIMEOUT_VALUES = {
        30_000L, 60_000L, 120_000L, 300_000L, 600_000L,
        1_800_000L, 3_600_000L, 7_200_000L, 14_400_000L, 28_800_000L, 43_200_000L, 86_400_000L
    };

    private void setupTimeoutPreference() {
        Preference timeoutPref = findPreference(TIMEOUT_KEY);
        if (timeoutPref == null) return;

        // Set current value as summary
        long currentTimeout = mViewModel.getClipboardAutoClearTimeout();
        timeoutPref.setSummary(mViewModel.formatTimeout(currentTimeout));

        timeoutPref.setOnPreferenceClickListener(v -> {
            showTimeoutDialog();
            return true;
        });
    }

    private void showTimeoutDialog() {
        long currentTimeout = mViewModel.getClipboardAutoClearTimeout();
        int selectedIndex = 6; // Default to 1 hour
        for (int i = 0; i < TIMEOUT_VALUES.length; i++) {
            if (TIMEOUT_VALUES[i] == currentTimeout) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
            .setTitle(R.string.clipboard_auto_clear_timeout_title)
            .setSingleChoiceItems(TIMEOUT_LABELS, selectedIndex, (dialog, which) -> {
                mViewModel.setClipboardAutoClearTimeout(TIMEOUT_VALUES[which]);
                Preference timeoutPref = findPreference(TIMEOUT_KEY);
                if (timeoutPref != null) {
                    timeoutPref.setSummary(TIMEOUT_LABELS[which]);
                }
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
}
