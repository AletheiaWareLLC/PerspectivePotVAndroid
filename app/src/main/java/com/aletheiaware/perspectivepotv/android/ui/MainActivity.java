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
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.BuildConfig;
import com.aletheiaware.perspectivepotv.android.R;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button playButton;
    private Button settingsButton;
    private Button debugButton;
    private ImageButton logoButton;
    public AlertDialog legaleseDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = findViewById(R.id.main_play_button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setEnabled(false);
                // Check legalese has been accepted
                if (Boolean.parseBoolean(CommonAndroidUtils.getPreference(MainActivity.this, getString(R.string.preference_legalese_accepted), "false"))) {
                    play();
                } else {
                    showLegalese(new Runnable() {
                        @Override
                        public void run() {
                            play();
                        }
                    });
                }
            }
        });

        settingsButton = findViewById(R.id.main_settings_button);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsButton.setEnabled(false);
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        debugButton = findViewById(R.id.main_debug_button);
        if (BuildConfig.DEBUG) {
            debugButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    debugButton.setEnabled(false);
                    Intent intent = new Intent(MainActivity.this, DebugActivity.class);
                    startActivity(intent);
                }
            });
            debugButton.setVisibility(View.VISIBLE);
        }

        logoButton = findViewById(R.id.aletheia_ware_llc_logo);
        logoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoButton.setEnabled(false);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aletheiaware.com"));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        playButton.setEnabled(hasFocus);
        settingsButton.setEnabled(hasFocus);
        debugButton.setEnabled(hasFocus);
        logoButton.setEnabled(hasFocus);
    }

    private void play() {
        // Check tutorial has been completed
        if (Boolean.parseBoolean(CommonAndroidUtils.getPreference(MainActivity.this, getString(R.string.preference_tutorial_completed), "false"))) {
            Intent intent = new Intent(MainActivity.this, WorldSelectActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, PerspectiveUtils.WORLD_TUTORIAL);
            intent.putExtra(PerspectiveAndroidUtils.PUZZLE_EXTRA, 1);
            startActivity(intent);
        }
    }

    public void showLegalese(final Runnable runnable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.LegaleseDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_legalese, null);
        TextView legaleseLabel = view.findViewById(R.id.legalese_label);
        legaleseLabel.setMovementMethod(LinkMovementMethod.getInstance());
        TextView legaleseBetaLabel = view.findViewById(R.id.legalese_beta_label);
        legaleseBetaLabel.setMovementMethod(LinkMovementMethod.getInstance());
        legaleseBetaLabel.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        Button accept = view.findViewById(R.id.main_legalese_accept);
        Button reject = view.findViewById(R.id.main_legalese_reject);
        builder.setView(view);
        legaleseDialog = builder.create();
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonAndroidUtils.setPreference(MainActivity.this, getString(R.string.preference_legalese_accepted), "true");
                legaleseDialog.cancel();
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                legaleseDialog.cancel();
            }
        });
        legaleseDialog.show();
    }
}
