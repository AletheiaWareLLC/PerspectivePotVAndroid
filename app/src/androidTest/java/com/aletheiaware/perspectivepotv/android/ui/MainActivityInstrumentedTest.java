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

import android.Manifest;
import android.content.Intent;
import android.view.View;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.perspectivepotv.android.ui.MainActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {

    private IntentsTestRule<MainActivity> intentsTestRule = new IntentsTestRule<>(MainActivity.class, true, false);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(intentsTestRule);

    @Test
    public void screenshot() throws Exception {
        MainActivity activity = intentsTestRule.launchActivity(new Intent());
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, "com.aletheiaware.perspectivepotv.android.ui.MainActivity.png");
    }

    @Test
    public void screenshot_release() throws Exception {
        final MainActivity activity = intentsTestRule.launchActivity(new Intent());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.debugButton.setVisibility(View.GONE);// Hide debug option
            }
        });
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, "com.aletheiaware.perspectivepotv.android.ui.MainActivity-release.png");
    }

    @Test
    public void screenshotLegalese() throws Exception {
        final MainActivity activity = intentsTestRule.launchActivity(new Intent());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.showLegalese(null);
            }
        });
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.legaleseDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.MainActivity-legalese.png");
    }
}
