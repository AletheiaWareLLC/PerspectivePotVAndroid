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
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.ui.WorldSelectActivity;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorldSelectActivityInstrumentedTest {

    private final IntentsTestRule<WorldSelectActivity> intentsTestRule = new IntentsTestRule<>(WorldSelectActivity.class, true, false);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .around(intentsTestRule);

    @Test
    public void screenshot() throws Exception {
        WorldSelectActivity activity = intentsTestRule.launchActivity(new Intent());
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, "com.aletheiaware.perspectivepotv.android.ui.WorldSelectActivity.png");
    }

    @Test
    public void screenshotPuzzleList() throws Exception {
        final WorldSelectActivity activity = intentsTestRule.launchActivity(new Intent());
        final IOException[] exception = new IOException[1];
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    activity.onSelect(PerspectiveAndroidUtils.getWorld(activity.getAssets(), PerspectiveUtils.WORLD_TUTORIAL));
                } catch (IOException e) {
                    exception[0] = e;
                }
            }
        });
        Thread.sleep(1000);
        if (exception[0] != null) {
            throw exception[0];
        }
        CommonAndroidUtils.captureScreenshot(activity, activity.puzzleListDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.WorldSelectActivity-puzzle-list.png");
    }

    @Test
    public void screenshotSKUPurchased() throws Exception {
        WorldSelectActivity activity = intentsTestRule.launchActivity(new Intent());
        List<String> skus = new ArrayList<>();
        skus.add("android.test.purchased");
        activity.querySkuDetails(skus);
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, "com.aletheiaware.perspectivepotv.android.ui.WorldSelectActivity-sku-purchased.png");
    }

    @Test
    public void screenshotSKUCanceled() throws Exception {
        WorldSelectActivity activity = intentsTestRule.launchActivity(new Intent());
        List<String> skus = new ArrayList<>();
        skus.add("android.test.canceled");
        activity.querySkuDetails(skus);
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, "com.aletheiaware.perspectivepotv.android.ui.WorldSelectActivity-sku-canceled.png");
    }

    @Test
    public void screenshotSKUItemUnavailable() throws Exception {
        WorldSelectActivity activity = intentsTestRule.launchActivity(new Intent());
        List<String> skus = new ArrayList<>();
        skus.add("android.test.item_unavailable");
        activity.querySkuDetails(skus);
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, "com.aletheiaware.perspectivepotv.android.ui.WorldSelectActivity-sku-item_unavailable.png");
    }
}
