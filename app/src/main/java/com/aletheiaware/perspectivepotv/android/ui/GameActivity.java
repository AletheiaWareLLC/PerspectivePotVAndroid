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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.common.utils.CommonUtils;
import com.aletheiaware.joy.android.scene.GLScene;
import com.aletheiaware.joy.scene.AttributeNode;
import com.aletheiaware.joy.scene.SceneGraphNode;
import com.aletheiaware.perspective.Perspective;
import com.aletheiaware.perspective.PerspectiveProto.Puzzle;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.PerspectiveProto.World;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.R;
import com.aletheiaware.perspectivepotv.android.billing.BillingManager;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class GameActivity extends AppCompatActivity implements Perspective.Callback, BillingManager.Callback {

    private static final String BUTTON_SOUND = "Button.wav";
    private static final String LAUNCH_SOUND = "Engine.wav";
    private static final String LANDING_SOUND = "Click2.wav";
    private static final String TURN_SOUND = "Click1.wav";
    private static final String SUCCESS_SOUND = "Success.wav";
    private static final String FAILURE_SOUND = "Failure.wav";
    private static final String STAR_SOUND = "star sound.wav";

    private static final long STAR_VIBRATION_GAP = 60;
    private static final long[][] STAR_VIBRATIONS = {
            {0, 70},
            {0, 70, STAR_VIBRATION_GAP, 90},
            {0, 70, STAR_VIBRATION_GAP, 90, STAR_VIBRATION_GAP, 115},
            {0, 70, STAR_VIBRATION_GAP, 90, STAR_VIBRATION_GAP, 115, STAR_VIBRATION_GAP, 145},
            {0, 70, STAR_VIBRATION_GAP, 90, STAR_VIBRATION_GAP, 115, STAR_VIBRATION_GAP, 145, STAR_VIBRATION_GAP, 180},
    };
    private static final long[] TRAVEL_VIBRATION = {0, 10};
    private static final long[] LAUNCH_VIBRATION = {0, 100};
    private static final long[] LANDING_VIBRATION = {0, 10};
    private static final long[] TURN_VIBRATION = {0, 10};

    public AlertDialog gameOverDialog;
    public AlertDialog gameMenuDialog;
    private String worldName;
    private int puzzleIndex;
    private boolean outlineEnabled;
    private World world;
    private Button launchButton;
    private GameView gameView;
    private TextView gameMoveCount;
    private GLScene glScene;
    private Perspective perspective;
    private SharedPreferences preferences;
    private Vibrator vibrator;
    private BillingManager manager;
    private MediaPlayer mediaPlayer;
    private SoundPool soundPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_loading);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        manager = new BillingManager(this, this);

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                System.out.println("SoundPool.onLoadComplete: " + sampleId + " " + status);
            }
        });

        if (savedInstanceState != null) {
            load(savedInstanceState);
        } else {
            final Intent intent = getIntent();
            if (intent != null) {
                Bundle data = intent.getExtras();
                if (data != null) {
                    load(data);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PerspectiveAndroidUtils.WORLD_EXTRA, worldName);
        outState.putInt(PerspectiveAndroidUtils.PUZZLE_EXTRA, puzzleIndex);
        outState.putBoolean(PerspectiveAndroidUtils.OUTLINE_EXTRA, outlineEnabled);
    }

    private void load(Bundle data) {
        worldName = data.getString(PerspectiveAndroidUtils.WORLD_EXTRA);
        puzzleIndex = data.getInt(PerspectiveAndroidUtils.PUZZLE_EXTRA);
        if (data.containsKey(PerspectiveAndroidUtils.ORIENTATION_EXTRA)) {
            setRequestedOrientation(data.getInt(PerspectiveAndroidUtils.ORIENTATION_EXTRA));
        }
        if (puzzleIndex < 1) {
            puzzleIndex = 1;
        }
        outlineEnabled = PerspectiveUtils.isTutorial(worldName)
                || data.getBoolean(PerspectiveAndroidUtils.OUTLINE_EXTRA)
                || preferences.getBoolean(getString(R.string.preference_puzzle_outline_key), true);

        final AssetManager assets = getAssets();
        // List Assets
        new Thread() {
            @Override
            public void run() {
                try {
                    String[] dir = assets.list("/");
                    if (dir != null) {
                        for (String d : dir) {
                            String[] file = assets.list("/"+d);
                            if (file != null) {
                                for (String f : file) {
                                    System.out.println("Asset: " + d + "/" + f);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Load Background Music
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                    AssetFileDescriptor afd = getAssets().openFd("music/MainThemeV2.wav");
                    mediaPlayer = createMediaPlayer();
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    mediaPlayer.setLooping(true);
                    afd.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Load Sound Effects
        new Thread() {
            @Override
            public void run() {
                try {
                    String[] sounds = assets.list("sound/");
                    if (sounds != null) {
                        for (String s : sounds) {
                            System.out.println("Sound Name: " + s);
                            try (AssetFileDescriptor fd = assets.openFd("sound/" + s)) {
                                int id = soundPool.load(fd, 1);
                                System.out.println("Sound ID: " + id);
                                glScene.putIntArray(s, new int[]{id});
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        // Load World, Scene, and Perspective in Worker Thread
        new Thread() {
            @Override
            public void run() {
                try {
                    Log.d(PerspectiveUtils.TAG, "Loading World");
                    world = PerspectiveAndroidUtils.getWorld(assets, worldName);
                    Log.d(PerspectiveUtils.TAG, "Creating Scene");
                    glScene = new GLScene();
                    perspective = new Perspective(GameActivity.this, glScene, world.getSize());
                    perspective.scenegraphs.putAll(PerspectiveAndroidUtils.createSceneGraphs(glScene, world));
                    perspective.outlineEnabled = outlineEnabled;
                    float[] background = PerspectiveUtils.BLACK;
                    String colour = world.getBackgroundColour();
                    if (colour != null && !colour.isEmpty()) {
                        background = glScene.getFloatArray(colour);
                    }
                    glScene.putFloatArray(GLScene.BACKGROUND, background);

                    // Create Game View in UI Thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(PerspectiveUtils.TAG, "Creating View");
                            setContentView(R.layout.activity_game);
                            gameView = findViewById(R.id.game_view);
                            gameView.setScene(glScene);
                            gameView.setPerspective(perspective);
                            ImageButton menuButton = findViewById(R.id.game_menu_button);
                            menuButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    sound(BUTTON_SOUND, 0);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameMenuDialogTheme);
                                    View layout = getLayoutInflater().inflate(R.layout.dialog_game_menu, null);
                                    builder.setView(layout);
                                    builder.setNegativeButton(R.string.game_reset, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                            perspective.clearAllLocations();
                                            loadPuzzle();
                                        }
                                    });
                                    builder.setPositiveButton(R.string.game_continue, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                                    builder.setNeutralButton(R.string.game_menu, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                            setResult(RESULT_CANCELED);
                                            finish();
                                        }
                                    });
                                    builder.setCancelable(false);
                                    gameMenuDialog = builder.create();
                                    gameMenuDialog.show();
                                }
                            });
                            gameMoveCount = findViewById(R.id.game_move_count);
                            launchButton = findViewById(R.id.game_launch_button);
                            launchButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    gameView.launch();
                                }
                            });
                            loadPuzzle();
                        }
                    });
                } catch (IOException e) {
                    CommonAndroidUtils.showErrorDialog(GameActivity.this, R.style.ErrorDialogTheme, R.string.error_game_init, e);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    public GLScene getGlScene() {
        return glScene;
    }

    public Perspective getPerspective() {
        return perspective;
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.quit();
            gameView = null;
        }
        if (manager != null) {
            manager.destroy();
            manager = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }

    private void sound(String name, int loop) {
        System.out.println("Sound Name: " + name);
        int[] ids = glScene.getIntArray(name);
        System.out.println("Sound ID: " + Arrays.toString(ids));
        if (ids != null && ids.length > 0) {
            int result = soundPool.play(ids[0], 1, 1, 1, loop, 1);
            System.out.println("Playing Sound Result: " + result);
        }
    }

    private void vibrate(long[] pattern) {
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.d(PerspectiveUtils.TAG, "No vibrator");
        } else if (preferences.getBoolean(getString(R.string.preference_puzzle_vibration_key), true)) {
            vibrator.vibrate(pattern, -1);
        }
    }

    private MediaPlayer createMediaPlayer() {
        MediaPlayer mp = new MediaPlayer();
        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                System.out.println("onInfo: " + mp.toString() + " " + what + " " + extra);
                return false;
            }
        });
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                System.out.println("onBufferingUpdate: " + mp.toString() + " " + percent);
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                System.out.println("onCompletion: " + mp.toString());
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                System.out.println("onError: " + mp.toString() + " " + what + " " + extra);
                return false;
            }
        });
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                System.out.println("onPrepared: " + mp.toString());
            }
        });
        return mp;
    }

    @UiThread
    private void loadPuzzle() {
        Log.d(PerspectiveUtils.TAG, "Loading Puzzle: " + puzzleIndex);
        new Thread() {
            @Override
            public void run() {
                final Puzzle puzzle = PerspectiveUtils.getPuzzle(world, puzzleIndex);
                if (puzzle != null) {
                    perspective.importPuzzle(puzzle);
                    final String name = CommonUtils.capitalize(world.getName()) + " - " + puzzleIndex;
                    final String title = CommonUtils.capitalize(world.getTitle());
                    final String description = puzzle.getDescription();
                    final int foreground = colourStringToInt(world.getForegroundColour());
                    //final int background = colourStringToInt(world.getBackgroundColour());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateMoveCount(0, puzzle.getTarget());
                            TextView nameText = findViewById(R.id.game_puzzle_name);
                            nameText.setText(name);
                            nameText.setTextColor(foreground);
                            TextView titleText = findViewById(R.id.game_puzzle_title);
                            titleText.setText(title);
                            titleText.setTextColor(foreground);
                            TextView descriptionText = findViewById(R.id.game_puzzle_description);
                            if (description != null && !description.isEmpty()) {
                                descriptionText.setText(description);
                                descriptionText.setTextColor(foreground);
                                descriptionText.setVisibility(View.VISIBLE);
                            } else {
                                descriptionText.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            }
        }.start();
    }

    private int colourStringToInt(String colour) {
        float[] array = PerspectiveUtils.WHITE;
        if (colour != null && !colour.isEmpty()) {
            array = glScene.getFloatArray(colour);
        }
        int red = (int) (array[0] * 255f);
        int green = (int) (array[1] * 255f);
        int blue = (int) (array[2] * 255f);
        return 0xff << 24 | (red & 0xff) << 16 | (green & 0xff) << 8 | (blue & 0xff);
    }

    @UiThread
    public void updateMoveCount(int moves, int target) {
        gameMoveCount.setText(getString(R.string.game_move_format, moves, target));
        if (moves <= target) {
            gameMoveCount.setTextColor(colourStringToInt("green"));
        } else if (moves <= target+PerspectiveUtils.MAX_STARS) {
            gameMoveCount.setTextColor(colourStringToInt("yellow"));
        } else {
            gameMoveCount.setTextColor(colourStringToInt("red"));
        }
    }

    @Override
    public SceneGraphNode getSceneGraphNode(String program, String name, String type, String mesh) {
        try {
            return PerspectiveAndroidUtils.getSceneGraphNode(glScene, getAssets(), program, name, type, mesh);
        } catch (IOException e) {
            CommonAndroidUtils.showErrorDialog(this, R.style.ErrorDialogTheme, R.string.error_get_scene_graph_node, e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AttributeNode getAttributeNode(String program, String name, String type, String colour, String material, String texture) {
        return PerspectiveAndroidUtils.getAttributeNode(glScene, getAssets(), program, type, colour, material, texture);
    }

    @Override
    public void onTravelStart() {
        Log.d(PerspectiveUtils.TAG, "Travel Start");
        vibrate(LAUNCH_VIBRATION);
        sound(LAUNCH_SOUND, 0);
    }

    @Override
    public void onTravelComplete() {
        Log.d(PerspectiveUtils.TAG, "Travel Complete");
        vibrate(LANDING_VIBRATION);
        sound(LANDING_SOUND, 0);
    }

    @Override
    public void onTurnComplete() {
        Log.d(PerspectiveUtils.TAG, "Turn Complete");
        vibrate(TURN_VIBRATION);
        sound(TURN_SOUND, 0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Solution solution = perspective.getSolution();
                int moves = 0;
                if (solution != null) {
                    moves = solution.getScore();
                }
                Puzzle puzzle = perspective.puzzle;
                int target = 0;
                if (puzzle != null) {
                    target = puzzle.getTarget();
                }
                updateMoveCount(moves, target);
            }
        });
    }

    @Override
    public void onGameLost() {
        Log.d(PerspectiveUtils.TAG, "Game Lost");
        sound(FAILURE_SOUND, 0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameLostDialogTheme);
                builder.setView(getLayoutInflater().inflate(R.layout.dialog_game_lost, null));
                builder.setPositiveButton(R.string.puzzle_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        perspective.clearAllLocations();
                        loadPuzzle();
                    }
                });
                builder.setNeutralButton(R.string.game_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                builder.setCancelable(false);
                gameOverDialog = builder.create();
                gameOverDialog.show();
            }
        });
    }

    @Override
    public void onGameWon() {
        Log.d(PerspectiveUtils.TAG, "Game Won");
        sound(SUCCESS_SOUND, 0);
        final Solution solution = perspective.getSolution();
        int target = perspective.puzzle.getTarget();
        int score = solution.getScore();
        Log.d(PerspectiveUtils.TAG, "Score: " + score + " (" + target + ")");
        final int stars = PerspectiveUtils.scoreToStars(score, target);
        Log.d(PerspectiveUtils.TAG, "Stars: " + stars);
        new Thread() {
            @Override
            public void run() {
                try {
                    String hash = PerspectiveUtils.getHash(perspective.puzzle.toByteArray());
                    PerspectiveAndroidUtils.saveSolution(GameActivity.this, worldName, hash, solution);
                } catch (IOException | NoSuchAlgorithmException e) {
                    CommonAndroidUtils.showErrorDialog(GameActivity.this, R.style.ErrorDialogTheme, R.string.error_save_solution, e);
                    e.printStackTrace();
                }
            }
        }.start();

        if (PerspectiveUtils.isTutorial(worldName)) {
            CommonAndroidUtils.setPreference(GameActivity.this, getString(R.string.preference_tutorial_completed), "true");
        }

        // Vibrate once for each star earned
        if (stars > 0) {
            vibrate(STAR_VIBRATIONS[stars - 1]);
            sound(STAR_SOUND, stars);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameWonDialogTheme);
                View view = getLayoutInflater().inflate(R.layout.dialog_game_won, null);
                builder.setView(view);
                view.findViewById(R.id.game_won_star1).setVisibility((stars > 0) ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.game_won_star2).setVisibility((stars > 1) ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.game_won_star3).setVisibility((stars > 2) ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.game_won_star4).setVisibility((stars > 3) ? View.VISIBLE : View.GONE);
                view.findViewById(R.id.game_won_star5).setVisibility((stars > 4) ? View.VISIBLE : View.GONE);
                if (puzzleIndex < world.getPuzzleCount()) {
                    builder.setPositiveButton(R.string.puzzle_next, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            perspective.clearAllLocations();
                            puzzleIndex++;
                            loadPuzzle();
                        }
                    });
                } else {
                    final String nextWorld = getNextWorld();
                    if (!nextWorld.equals(PerspectiveUtils.WORLD_TUTORIAL)) {
                        builder.setPositiveButton(R.string.puzzle_next, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                worldName = nextWorld;
                                puzzleIndex = 1;
                                setResult(RESULT_OK);
                                finish();
                                Intent intent = new Intent(GameActivity.this, GameActivity.class);
                                intent.putExtra(PerspectiveAndroidUtils.WORLD_EXTRA, worldName);
                                intent.putExtra(PerspectiveAndroidUtils.PUZZLE_EXTRA, puzzleIndex);
                                startActivity(intent);
                            }
                        });
                    }
                }
                if (stars < 5) {
                    builder.setNegativeButton(R.string.puzzle_retry, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            perspective.clearAllLocations();
                            loadPuzzle();
                        }
                    });
                }
                builder.setNeutralButton(R.string.game_menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
                builder.setCancelable(false);
                gameOverDialog = builder.create();
                gameOverDialog.show();
            }
        });
    }

    @NonNull
    private String getNextWorld() {
        switch (worldName) {
            case PerspectiveUtils.WORLD_TUTORIAL:
                return PerspectiveUtils.WORLD_ONE;
            case PerspectiveUtils.WORLD_ONE:
                return PerspectiveUtils.WORLD_TWO;
            case PerspectiveUtils.WORLD_TWO:
                return PerspectiveUtils.WORLD_THREE;
            case PerspectiveUtils.WORLD_THREE:
                return PerspectiveUtils.WORLD_FOUR;
            case PerspectiveUtils.WORLD_FOUR:
                return PerspectiveUtils.WORLD_FIVE;
            case PerspectiveUtils.WORLD_FIVE:
                return PerspectiveUtils.WORLD_SIX;
            case PerspectiveUtils.WORLD_SIX:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_SEVEN)) {
                    return PerspectiveUtils.WORLD_SEVEN;
                } // else fallthrough
            case PerspectiveUtils.WORLD_SEVEN:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_EIGHT)) {
                    return PerspectiveUtils.WORLD_EIGHT;
                } // else fallthrough
            case PerspectiveUtils.WORLD_EIGHT:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_NINE)) {
                    return PerspectiveUtils.WORLD_NINE;
                } // else fallthrough
            case PerspectiveUtils.WORLD_NINE:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_TEN)) {
                    return PerspectiveUtils.WORLD_TEN;
                } // else fallthrough
            case PerspectiveUtils.WORLD_TEN:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_ELEVEN)) {
                    return PerspectiveUtils.WORLD_ELEVEN;
                } // else fallthrough
            case PerspectiveUtils.WORLD_ELEVEN:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_TWELVE)) {
                    return PerspectiveUtils.WORLD_TWELVE;
                } // else fallthrough
            case PerspectiveUtils.WORLD_TWELVE:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_THIRTEEN)) {
                    return PerspectiveUtils.WORLD_THIRTEEN;
                } // else fallthrough
            case PerspectiveUtils.WORLD_THIRTEEN:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_FOURTEEN)) {
                    return PerspectiveUtils.WORLD_FOURTEEN;
                } // else fallthrough
            case PerspectiveUtils.WORLD_FOURTEEN:
                if (manager.hasPurchased(PerspectiveUtils.WORLD_FIFTEEN)) {
                    return PerspectiveUtils.WORLD_FIFTEEN;
                } // else fallthrough
            default:
                return PerspectiveUtils.WORLD_TUTORIAL;
        }
    }

    @Override
    public void onBillingClientSetup() {
        Log.d(PerspectiveUtils.TAG, "Billing Client Setup");
        // TODO
    }

    @Override
    public void onPurchasesUpdated() {
        Log.d(PerspectiveUtils.TAG, "Purchases Updated");
        // TODO
    }

    @Override
    public void onTokenConsumed(String purchaseToken) {
        Log.d(PerspectiveUtils.TAG, "Token Consumed: " + purchaseToken);
        // TODO
    }
}
