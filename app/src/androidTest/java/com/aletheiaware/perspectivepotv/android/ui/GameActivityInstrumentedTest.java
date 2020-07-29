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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.joy.android.scene.GLScene;
import com.aletheiaware.perspective.Perspective;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

@RunWith(AndroidJUnit4.class)
public class GameActivityInstrumentedTest {

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    @Rule
    public IntentsTestRule<GameActivity> intentsTestRule = new IntentsTestRule<>(GameActivity.class, true, false);

    private static PowerManager.WakeLock wakeLock;

    @BeforeClass
    public static void setUpClass() {
        PowerManager power = (PowerManager) InstrumentationRegistry.getInstrumentation().getTargetContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = power.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "GameActivityInstrumentedTest");
        wakeLock.acquire();
    }

    @AfterClass
    public static void tearDownClass() {
        wakeLock.release();
    }

    @Test
    public void screenshotBanner() throws Exception {
        Intent intent = createIconicIntent();
        intent.putExtra(PerspectiveAndroidUtils.ORIENTATION_EXTRA, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        GLScene scene = activity.getGlScene();
        setIconicScene(scene, activity.getPerspective());
        Thread.sleep(1000);
        captureScreenshot(scene, "com.aletheiaware.perspectivepotv.android.ui.GameActivity-banner.png");
    }

    @Test
    public void screenshotLogo() throws Exception {
        Intent intent = createIconicIntent();
        intent.putExtra(PerspectiveAndroidUtils.ORIENTATION_EXTRA, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        GLScene scene = activity.getGlScene();
        setIconicScene(scene, activity.getPerspective());
        Thread.sleep(1000);
        captureScreenshot(scene, "com.aletheiaware.perspectivepotv.android.ui.GameActivity-logo.png");
    }

    private Intent createIconicIntent() {
        Intent intent = new Intent();
        intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, PerspectiveUtils.WORLD_ONE);
        intent.putExtra(PerspectiveAndroidUtils.PUZZLE_EXTRA, 2);
        intent.putExtra(PerspectiveAndroidUtils.OUTLINE_EXTRA, true);
        return intent;
    }

    private Intent createTutorialIntent(int puzzle) {
        Intent intent = new Intent();
        intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, PerspectiveUtils.WORLD_TUTORIAL);
        intent.putExtra(PerspectiveAndroidUtils.PUZZLE_EXTRA, puzzle);
        intent.putExtra(PerspectiveAndroidUtils.OUTLINE_EXTRA, true);
        return intent;
    }

    private void setIconicScene(GLScene scene, Perspective perspective) {
        perspective.rotate(0, (float) (Math.PI / 4));
        perspective.rotate((float) (Math.PI / 8), 0);
        scene.putFloatArray(GLScene.BACKGROUND, PerspectiveUtils.PURPLE);
    }

    @Test
    public void screenshotGameHUDTutorial() throws Exception {
        Intent intent = createTutorialIntent(1);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity,"com.aletheiaware.perspectivepotv.android.ui.GameActivity-hud-tutorial.png");
        activity.finish();
    }

    @Test
    public void screenshotGameHUDWorldFree() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, PerspectiveUtils.WORLD_SIX);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity,"com.aletheiaware.perspectivepotv.android.ui.GameActivity-hud-world-free.png");
        activity.finish();
    }

    @Test
    public void screenshotGameHUDWorldPaid() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, PerspectiveUtils.WORLD_SEVEN);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity,"com.aletheiaware.perspectivepotv.android.ui.GameActivity-hud-world-paid.png");
        activity.finish();
    }

    @Test
    public void screenshotGameMenu() throws Exception {
        Intent intent = createTutorialIntent(1);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        activity.onGameMenu();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameMenuDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-menu.png");
        activity.finish();
    }

    @Test
    public void screenshotGameLost() throws Exception {
        Intent intent = createTutorialIntent(1);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        activity.onGameLost();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-lost.png");
        activity.finish();
    }

    @Test
    public void screenshotGameWon0Stars() throws Exception {
        Intent intent = createTutorialIntent(0);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Perspective perspective = activity.getPerspective();
        perspective.solution.setScore(perspective.puzzle.getTarget()+10);
        activity.onGameWon();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-won-0-stars.png");
        activity.finish();
    }

    @Test
    public void screenshotGameWon1Star() throws Exception {
        Intent intent = createTutorialIntent(0);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Perspective perspective = activity.getPerspective();
        perspective.solution.setScore(perspective.puzzle.getTarget()+4);
        activity.onGameWon();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-won-1-star.png");
        activity.finish();
    }

    @Test
    public void screenshotGameWon2Stars() throws Exception {
        Intent intent = createTutorialIntent(0);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Perspective perspective = activity.getPerspective();
        perspective.solution.setScore(perspective.puzzle.getTarget()+3);
        activity.onGameWon();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-won-2-stars.png");
        activity.finish();
    }

    @Test
    public void screenshotGameWon3Stars() throws Exception {
        Intent intent = createTutorialIntent(0);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Perspective perspective = activity.getPerspective();
        perspective.solution.setScore(perspective.puzzle.getTarget()+2);
        activity.onGameWon();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-won-3-stars.png");
        activity.finish();
    }

    @Test
    public void screenshotGameWon4Stars() throws Exception {
        Intent intent = createTutorialIntent(0);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Perspective perspective = activity.getPerspective();
        perspective.solution.setScore(perspective.puzzle.getTarget()+1);
        activity.onGameWon();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-won-4-stars.png");
        activity.finish();
    }

    @Test
    public void screenshotGameWon5Stars() throws Exception {
        Intent intent = createTutorialIntent(0);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Perspective perspective = activity.getPerspective();
        perspective.solution.setScore(perspective.puzzle.getTarget());
        activity.onGameWon();
        Thread.sleep(1000);
        CommonAndroidUtils.captureScreenshot(activity, activity.gameOverDialog.getWindow(), "com.aletheiaware.perspectivepotv.android.ui.GameActivity-game-won-5-stars.png");
        activity.finish();
    }

    @Test
    public void screenshotTutorialPuzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TUTORIAL, 1);
    }

    @Test
    public void screenshotTutorialPuzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TUTORIAL, 2);
    }

    @Test
    public void screenshotTutorialPuzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TUTORIAL, 3);
    }

    @Test
    public void screenshotTutorialPuzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TUTORIAL, 4);
    }

    @Test
    public void screenshotTutorialPuzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TUTORIAL, 5);
    }

    @Test
    public void screenshotTutorialPuzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TUTORIAL, 6);
    }

    @Test
    public void screenshotWorld1Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ONE, 1);
    }

    @Test
    public void screenshotWorld1Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ONE, 2);
    }

    @Test
    public void screenshotWorld1Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ONE, 3);
    }

    @Test
    public void screenshotWorld1Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ONE, 4);
    }

    @Test
    public void screenshotWorld1Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ONE, 5);
    }

    @Test
    public void screenshotWorld1Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ONE, 6);
    }

    @Test
    public void screenshotWorld2Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 1);
    }

    @Test
    public void screenshotWorld2Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 2);
    }

    @Test
    public void screenshotWorld2Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 3);
    }

    @Test
    public void screenshotWorld2Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 4);
    }

    @Test
    public void screenshotWorld2Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 5);
    }

    @Test
    public void screenshotWorld2Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 6);
    }

    @Test
    public void screenshotWorld2Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 7);
    }

    @Test
    public void screenshotWorld2Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 8);
    }

    @Test
    public void screenshotWorld2Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 9);
    }

    @Test
    public void screenshotWorld2Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld2Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld2Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWO, 12);
    }
 
    @Test
    public void screenshotWorld3Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 1);
    }

    @Test
    public void screenshotWorld3Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 2);
    }

    @Test
    public void screenshotWorld3Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 3);
    }

    @Test
    public void screenshotWorld3Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 4);
    }

    @Test
    public void screenshotWorld3Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 5);
    }

    @Test
    public void screenshotWorld3Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 6);
    }

    @Test
    public void screenshotWorld3Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 7);
    }

    @Test
    public void screenshotWorld3Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 8);
    }

    @Test
    public void screenshotWorld3Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 9);
    }

    @Test
    public void screenshotWorld3Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld3Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld3Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THREE, 12);
    }

    @Test
    public void screenshotWorld4Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 1);
    }

    @Test
    public void screenshotWorld4Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 2);
    }

    @Test
    public void screenshotWorld4Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 3);
    }

    @Test
    public void screenshotWorld4Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 4);
    }

    @Test
    public void screenshotWorld4Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 5);
    }

    @Test
    public void screenshotWorld4Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 6);
    }

    @Test
    public void screenshotWorld4Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 7);
    }

    @Test
    public void screenshotWorld4Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 8);
    }

    @Test
    public void screenshotWorld4Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 9);
    }

    @Test
    public void screenshotWorld4Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld4Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld4Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOUR, 12);
    }

    @Test
    public void screenshotWorld5Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 1);
    }

    @Test
    public void screenshotWorld5Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 2);
    }

    @Test
    public void screenshotWorld5Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 3);
    }

    @Test
    public void screenshotWorld5Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 4);
    }

    @Test
    public void screenshotWorld5Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 5);
    }

    @Test
    public void screenshotWorld5Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 6);
    }

    @Test
    public void screenshotWorld5Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 7);
    }

    @Test
    public void screenshotWorld5Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 8);
    }

    @Test
    public void screenshotWorld5Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 9);
    }

    @Test
    public void screenshotWorld5Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld5Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld5Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld5Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld5Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld5Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld5Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIVE, 16);
    }

    @Test
    public void screenshotWorld6Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 1);
    }

    @Test
    public void screenshotWorld6Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 2);
    }

    @Test
    public void screenshotWorld6Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 3);
    }

    @Test
    public void screenshotWorld6Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 4);
    }

    @Test
    public void screenshotWorld6Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 5);
    }

    @Test
    public void screenshotWorld6Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 6);
    }

    @Test
    public void screenshotWorld6Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 7);
    }

    @Test
    public void screenshotWorld6Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 8);
    }

    @Test
    public void screenshotWorld6Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 9);
    }

    @Test
    public void screenshotWorld6Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld6Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld6Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld6Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld6Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld6Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld6Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SIX, 16);
    }

    @Test
    public void screenshotWorld7Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 1);
    }

    @Test
    public void screenshotWorld7Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 2);
    }

    @Test
    public void screenshotWorld7Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 3);
    }

    @Test
    public void screenshotWorld7Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 4);
    }

    @Test
    public void screenshotWorld7Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 5);
    }

    @Test
    public void screenshotWorld7Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 6);
    }

    @Test
    public void screenshotWorld7Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 7);
    }

    @Test
    public void screenshotWorld7Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 8);
    }

    @Test
    public void screenshotWorld7Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 9);
    }

    @Test
    public void screenshotWorld7Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld7Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld7Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld7Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld7Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld7Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld7Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_SEVEN, 16);
    }

    @Test
    public void screenshotWorld8Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 1);
    }

    @Test
    public void screenshotWorld8Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 2);
    }

    @Test
    public void screenshotWorld8Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 3);
    }

    @Test
    public void screenshotWorld8Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 4);
    }

    @Test
    public void screenshotWorld8Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 5);
    }

    @Test
    public void screenshotWorld8Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 6);
    }

    @Test
    public void screenshotWorld8Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 7);
    }

    @Test
    public void screenshotWorld8Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 8);
    }

    @Test
    public void screenshotWorld8Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 9);
    }

    @Test
    public void screenshotWorld8Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld8Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld8Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld8Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld8Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld8Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld8Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_EIGHT, 16);
    }

    @Test
    public void screenshotWorld9Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 1);
    }

    @Test
    public void screenshotWorld9Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 2);
    }

    @Test
    public void screenshotWorld9Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 3);
    }

    @Test
    public void screenshotWorld9Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 4);
    }

    @Test
    public void screenshotWorld9Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 5);
    }

    @Test
    public void screenshotWorld9Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 6);
    }

    @Test
    public void screenshotWorld9Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 7);
    }

    @Test
    public void screenshotWorld9Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 8);
    }

    @Test
    public void screenshotWorld9Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 9);
    }

    @Test
    public void screenshotWorld9Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld9Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld9Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld9Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld9Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld9Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld9Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_NINE, 16);
    }
/*
    @Test
    public void screenshotWorld10Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 1);
    }

    @Test
    public void screenshotWorld10Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 2);
    }

    @Test
    public void screenshotWorld10Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 3);
    }

    @Test
    public void screenshotWorld10Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 4);
    }

    @Test
    public void screenshotWorld10Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 5);
    }

    @Test
    public void screenshotWorld10Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 6);
    }

    @Test
    public void screenshotWorld10Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 7);
    }

    @Test
    public void screenshotWorld10Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 8);
    }

    @Test
    public void screenshotWorld10Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 9);
    }

    @Test
    public void screenshotWorld10Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld10Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld10Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld10Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld10Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld10Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld10Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TEN, 16);
    }

    @Test
    public void screenshotWorld11Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 1);
    }

    @Test
    public void screenshotWorld11Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 2);
    }

    @Test
    public void screenshotWorld11Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 3);
    }

    @Test
    public void screenshotWorld11Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 4);
    }

    @Test
    public void screenshotWorld11Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 5);
    }

    @Test
    public void screenshotWorld11Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 6);
    }

    @Test
    public void screenshotWorld11Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 7);
    }

    @Test
    public void screenshotWorld11Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 8);
    }

    @Test
    public void screenshotWorld11Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 9);
    }

    @Test
    public void screenshotWorld11Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld11Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld11Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld11Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld11Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld11Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld11Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_ELEVEN, 16);
    }
 
    @Test
    public void screenshotWorld12Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 1);
    }

    @Test
    public void screenshotWorld12Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 2);
    }

    @Test
    public void screenshotWorld12Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 3);
    }

    @Test
    public void screenshotWorld12Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 4);
    }

    @Test
    public void screenshotWorld12Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 5);
    }

    @Test
    public void screenshotWorld12Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 6);
    }

    @Test
    public void screenshotWorld12Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 7);
    }

    @Test
    public void screenshotWorld12Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 8);
    }

    @Test
    public void screenshotWorld12Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 9);
    }

    @Test
    public void screenshotWorld12Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld12Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld12Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld12Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld12Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld12Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld12Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_TWELVE, 16);
    }

    @Test
    public void screenshotWorld13Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 1);
    }

    @Test
    public void screenshotWorld13Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 2);
    }

    @Test
    public void screenshotWorld13Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 3);
    }

    @Test
    public void screenshotWorld13Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 4);
    }

    @Test
    public void screenshotWorld13Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 5);
    }

    @Test
    public void screenshotWorld13Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 6);
    }

    @Test
    public void screenshotWorld13Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 7);
    }

    @Test
    public void screenshotWorld13Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 8);
    }

    @Test
    public void screenshotWorld13Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 9);
    }

    @Test
    public void screenshotWorld13Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld13Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld13Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld13Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld13Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld13Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld13Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_THIRTEEN, 16);
    }

    @Test
    public void screenshotWorld14Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 1);
    }

    @Test
    public void screenshotWorld14Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 2);
    }

    @Test
    public void screenshotWorld14Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 3);
    }

    @Test
    public void screenshotWorld14Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 4);
    }

    @Test
    public void screenshotWorld14Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 5);
    }

    @Test
    public void screenshotWorld14Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 6);
    }

    @Test
    public void screenshotWorld14Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 7);
    }

    @Test
    public void screenshotWorld14Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 8);
    }

    @Test
    public void screenshotWorld14Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 9);
    }

    @Test
    public void screenshotWorld14Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld14Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld14Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 12);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld14Puzzle13() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 13);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld14Puzzle14() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 14);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld14Puzzle15() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 15);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld14Puzzle16() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FOURTEEN, 16);
    }

    @Test
    public void screenshotWorld15Puzzle1() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 1);
    }

    @Test
    public void screenshotWorld15Puzzle2() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 2);
    }

    @Test
    public void screenshotWorld15Puzzle3() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 3);
    }

    @Test
    public void screenshotWorld15Puzzle4() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 4);
    }

    @Test
    public void screenshotWorld15Puzzle5() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 5);
    }

    @Test
    public void screenshotWorld15Puzzle6() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 6);
    }

    @Test
    public void screenshotWorld15Puzzle7() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 7);
    }

    @Test
    public void screenshotWorld15Puzzle8() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 8);
    }

    @Test
    public void screenshotWorld15Puzzle9() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 9);
    }

    @Test
    public void screenshotWorld15Puzzle10() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 10);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld15Puzzle11() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 11);
    }

    @Ignore("Not Generated")
    @Test
    public void screenshotWorld15Puzzle12() throws Exception {
        captureScreenshot(PerspectiveUtils.WORLD_FIFTEEN, 12);
    }
*/
    private void captureScreenshot(String world, int puzzle) throws Exception {
        String name = "com.aletheiaware.perspectivepotv.android.ui.GameActivity-" + world + "-" + puzzle + ".png";
        Log.d(PerspectiveUtils.TAG, "Capturing " + name);
        Intent intent = new Intent();
        intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, world);
        intent.putExtra(PerspectiveAndroidUtils.PUZZLE_EXTRA, puzzle);
        GameActivity activity = intentsTestRule.launchActivity(intent);
        activity.loadLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1000);
        captureScreenshot(activity.getGlScene(), name);
        activity.finish();
    }

    public void captureScreenshot(final GLScene scene, final String name) {
        final CountDownLatch latch = new CountDownLatch(1);
        scene.setFrameCallback(new GLScene.FrameCallback() {
            @Override
            public boolean onFrame() {
                Log.d(PerspectiveUtils.TAG, "Frame callback");
                int[] viewport = scene.getViewport();
                int x = viewport[0];
                int y = viewport[1];
                int w = viewport[2];
                int h = viewport[3];
                int[] array = new int[w * h];
                int[] source = new int[w * h];
                IntBuffer buffer = IntBuffer.wrap(array);
                buffer.position(0);
                GLES20.glReadPixels(x, y, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
                for (int i = 0; i < h; i++) {
                    int stride = i * w;
                    int offset = (h - i - 1) * w;
                    for (int j = 0; j < w; j++) {
                        int pixel = array[stride + j];
                        int alpha = pixel & 0xff000000;
                        int red = (pixel << 16) & 0x00ff0000;
                        int green = pixel & 0x0000ff00;
                        int blue = (pixel >> 16) & 0x000000ff;
                        source[offset + j] = alpha | red | green | blue;
                    }
                }
                Bitmap bitmap = Bitmap.createBitmap(source, w, h, Bitmap.Config.ARGB_8888);
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File file = new File(dir, name);
                try (FileOutputStream out = new FileOutputStream(file)){
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
                return false;
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
