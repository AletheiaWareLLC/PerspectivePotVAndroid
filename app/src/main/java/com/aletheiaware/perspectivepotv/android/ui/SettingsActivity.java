/*
 * Copyright 2020 Aletheia Ware LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aletheiaware.perspectivepotv.android.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.perspectivepotv.android.BuildConfig;
import com.aletheiaware.perspectivepotv.android.R;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;

import java.io.IOException;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    public SettingsPreferenceFragment preferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null || preferenceFragment == null) {
            preferenceFragment = new SettingsPreferenceFragment();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.preference_frame, preferenceFragment);
            ft.commit();
        }
    }

    public static class SettingsPreferenceFragment extends PreferenceFragmentCompat {
        public AlertDialog clearProgressDialog;
        private Preference clearProgressPreference;
        private Preference versionPreference;
        private Preference supportPreference;
        private Preference morePreference;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.fragment_preference);

            Preference outlinePreference = findPreference(getString(R.string.preference_puzzle_outline_key));
            setTint(outlinePreference);

            Preference musicPreference = findPreference(getString(R.string.preference_puzzle_music_key));
            setTint(musicPreference);

            Preference soundPreference = findPreference(getString(R.string.preference_puzzle_sound_key));
            setTint(soundPreference);

            Preference vibrationPreference = findPreference(getString(R.string.preference_puzzle_vibration_key));
            setTint(vibrationPreference);

            clearProgressPreference = findPreference(getString(R.string.preference_clear_progress_key));
            setTint(clearProgressPreference);
            clearProgressPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return false;
                    }
                    clearProgress(activity);
                    return true;
                }
            });

            versionPreference = findPreference(getString(R.string.preference_version_key));
            setTint(versionPreference);
            versionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.aletheiaware.perspectivepotv.android"));
                    startActivity(intent);
                    return true;
                }
            });

            supportPreference = findPreference(getString(R.string.preference_support_key));
            setTint(supportPreference);
            supportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return false;
                    }
                    CommonAndroidUtils.support(activity, new StringBuilder());
                    return true;
                }
            });

            morePreference = findPreference(getString(R.string.preference_more_key));
            setTint(morePreference);
            morePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id=Aletheia+Ware+LLC"));
                    startActivity(intent);
                    return true;
                }
            });

            update();
        }

        public void setTint(Preference preference) {
            if (preference != null) {
                Drawable icon = preference.getIcon();
                if (icon != null) {
                    icon.setTint(getResources().getColor(R.color.white));
                }
            }
        }

        public void clearProgress(final FragmentActivity activity) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.WarningDialogTheme);
            View layout = getLayoutInflater().inflate(R.layout.dialog_clear_progress, null);
            Button delete = layout.findViewById(R.id.settings_clear_progress);
            builder.setView(layout);
            clearProgressDialog = builder.create();
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clearProgressDialog.dismiss();
                    // TODO should this clear the outline_enabled_preference?
                    // TODO should this clear the music_enabled_preference?
                    // TODO should this clear the sound_enabled_preference?
                    // TODO should this clear the vibration_enabled_preference?
                    // TODO should this clear the legalese_accepted_preference?
                    CommonAndroidUtils.setPreference(activity, getString(R.string.preference_tutorial_completed), "false");
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                PerspectiveAndroidUtils.clearSolutions(activity);
                            } catch (IOException e) {
                                CommonAndroidUtils.showErrorDialog(activity, R.style.ErrorDialogTheme, R.string.error_clear_solutions, e);
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            });
            clearProgressDialog.show();
        }

        public void update() {
            final FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            versionPreference.setSummary(BuildConfig.BUILD_TYPE + "-" + BuildConfig.VERSION_NAME);
        }
    }
}
