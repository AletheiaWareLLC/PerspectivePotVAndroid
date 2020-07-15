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
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
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
import com.aletheiaware.joy.scene.SceneGraphNode;
import com.aletheiaware.joy.scene.Vector;
import com.aletheiaware.perspective.Perspective;
import com.aletheiaware.perspective.Perspective.Element;
import com.aletheiaware.perspective.PerspectiveProto.Dialog;
import com.aletheiaware.perspective.PerspectiveProto.Move;
import com.aletheiaware.perspective.PerspectiveProto.Puzzle;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.PerspectiveProto.World;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.R;
import com.aletheiaware.perspectivepotv.android.billing.BillingManager;
import com.aletheiaware.perspectivepotv.android.scene.LaunchAnimation;
import com.aletheiaware.perspectivepotv.android.utils.PerspectiveAndroidUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.preference.PreferenceManager;

public class GameActivity extends AppCompatActivity implements Perspective.Callback, BillingManager.Callback {

    private static final String THEME_MUSIC = "ThemeV2_3X.wav";

    private static final String BUTTON_SOUND = "Button.wav";
    private static final String LAUNCH_SOUND = "Engine.wav";
    private static final String LANDING_SOUND = "Click2.wav";
    private static final String TURN_SOUND = "Click1.wav";
    private static final String GOAL_LEVEL_SOUND = "Goal-level.wav";
    private static final String GOAL_WORLD_SOUND = "Goal-world.wav";
    private static final String FAILURE_SOUND = "Failure.wav";
    private static final String STAR_SOUND = "Star.wav";
    private static final String PORTAL_SOUND = "Portal.wav";
    private static final String JOURNAL_SOUND = "Journal.wav";

    private static final long STAR_VIBRATION_GAP = 60;
    private static final long[][] STAR_VIBRATIONS = {
            {0, 70},
            {0, 70, STAR_VIBRATION_GAP, 90},
            {0, 70, STAR_VIBRATION_GAP, 90, STAR_VIBRATION_GAP, 115},
            {0, 70, STAR_VIBRATION_GAP, 90, STAR_VIBRATION_GAP, 115, STAR_VIBRATION_GAP, 145},
            {0, 70, STAR_VIBRATION_GAP, 90, STAR_VIBRATION_GAP, 115, STAR_VIBRATION_GAP, 145, STAR_VIBRATION_GAP, 180},
    };
    private static final long[] LAUNCH_VIBRATION = {0, 100};
    private static final long[] LANDING_VIBRATION = {0, 10};
    private static final long[] TURN_VIBRATION = {0, 10};
    private static final long[] PORTAL_VIBRATION = {0, 10};

    public AlertDialog gameOverDialog;
    public AlertDialog gameMenuDialog;
    public AlertDialog gameDialogDialog;
    private String worldName;
    private int puzzleIndex;
    private boolean outlineEnabled;
    private World world;
    private CardView gameLaunchCard;
    private Button gameLaunchButton;
    private GameView gameView;
    private ImageButton gameMenuButton;
    private CardView gameMoveCountCard;
    private TextView gameMoveCountText;
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

        Log.d(PerspectiveUtils.TAG, "Creating Scene");
        glScene = new GLScene();

        final AssetManager assets = getAssets();
        // List Assets
        new Thread() {
            @Override
            public void run() {
                try {
                    String[] dir = assets.list("/");
                    if (dir != null) {
                        for (String d : dir) {
                            String[] file = assets.list("/" + d);
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
                    perspective = new Perspective(GameActivity.this, glScene, world.getSize());
                    perspective.scenegraphs.putAll(PerspectiveAndroidUtils.createSceneGraphs(glScene, world));
                    perspective.outlineEnabled = outlineEnabled;
                    // Background
                    float[] background = PerspectiveUtils.BLACK;
                    String colour = world.getBackgroundColour();
                    if (colour != null && !colour.isEmpty()) {
                        background = glScene.getFloatArray(colour);
                    }
                    glScene.putFloatArray(GLScene.BACKGROUND, background);
                    // Fog
                    glScene.putFloatArray("fog-colour", PerspectiveUtils.PURPLE);

                    // Create Game View in UI Thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(PerspectiveUtils.TAG, "Creating View");
                            setContentView(R.layout.activity_game);
                            gameView = findViewById(R.id.game_view);
                            gameView.setScene(glScene);
                            gameView.setPerspective(perspective);
                            gameMenuButton = findViewById(R.id.game_menu_button);
                            gameMenuButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    onGameMenu();
                                }
                            });
                            gameMoveCountCard = findViewById(R.id.game_move_count_card);
                            gameMoveCountText = findViewById(R.id.game_move_count_text);
                            gameLaunchCard = findViewById(R.id.game_launch_card);
                            gameLaunchButton = findViewById(R.id.game_launch_button);
                            gameLaunchButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    launch();
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
    protected void onResume() {
        super.onResume();
        if (preferences.getBoolean(getString(R.string.preference_puzzle_music_key), true)) {
            // Load Background Music
            new Thread() {
                @Override
                public void run() {
                    try {
                        AssetFileDescriptor afd = getAssets().openFd("music/" + THEME_MUSIC);
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
        } else {
            Log.d(PerspectiveUtils.TAG, "Music Disabled");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }

    private void sound(String name, int loop) {
        if (preferences.getBoolean(getString(R.string.preference_puzzle_sound_key), true)) {
            System.out.println("Sound Name: " + name);
            int[] ids = glScene.getIntArray(name);
            System.out.println("Sound ID: " + Arrays.toString(ids));
            if (ids != null && ids.length > 0) {
                int result = soundPool.play(ids[0], 1, 1, 1, loop, 1);
                System.out.println("Playing Sound Result: " + result);
            }
        } else {
            Log.d(PerspectiveUtils.TAG, "SFX Disabled");
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
                    checkDialogs();
                    final String name = CommonUtils.capitalize(world.getName()) + " - " + puzzleIndex;
                    final String title = CommonUtils.capitalize(world.getTitle());
                    final String description = puzzle.getDescription();
                    final int foreground = colourStringToInt(world.getForegroundColour());
                    final int background = colourStringToInt(world.getBackgroundColour());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gameLaunchButton.setTextColor(foreground);
                            gameLaunchCard.setCardBackgroundColor(background);
                            gameMoveCountText.setTextColor(foreground);
                            gameMoveCountCard.setCardBackgroundColor(background);
                            updateMoveCount(0, puzzle.getTarget());
                            ImageViewCompat.setImageTintList(gameMenuButton, ColorStateList.valueOf(foreground));
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
        gameMoveCountText.setText(getString(R.string.game_move_format, moves, target));
        if (moves < target) {
            return;
        }
        int id;
        if (moves == target) {
            id = R.color.green;
        } else if (moves <= target + PerspectiveUtils.MAX_STARS) {
            id = R.color.orange;
        } else {
            id = R.color.red;
        }
        gameMoveCountText.setTextColor(ContextCompat.getColor(this, id));
    }

    @Override
    public SceneGraphNode getSceneGraphNode(String shader, String name, String type, String mesh, String colour, String texture, String material) {
        try {
            return PerspectiveAndroidUtils.getSceneGraphNode(glScene, getAssets(), shader, name, type, mesh, colour, texture, material);
        } catch (IOException e) {
            CommonAndroidUtils.showErrorDialog(this, R.style.ErrorDialogTheme, R.string.error_get_scene_graph_node, e);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onDropComplete() {
        // Ignore
    }

    public void launch() {
        synchronized (perspective) {
            SceneGraphNode node = perspective.getSceneGraphNode();
            if (!node.hasAnimation()) {
                System.out.println("launch");
                vibrate(LAUNCH_VIBRATION);
                sound(LAUNCH_SOUND, 0);
                if (perspective.inverseRotation.makeInverse(perspective.mainRotation)) {
                    // TODO improve this - creating new sets and maps each time is expensive
                    Map<String, Vector> blocks = new HashMap<>();
                    List<Element> bs = perspective.getElements("block");
                    if (bs != null) {
                        for (Element b : bs) {
                            blocks.put(b.name, glScene.getVector(b.name));
                        }
                    }
                    final Map<String, Vector> goals = new HashMap<>();
                    List<Element> gs = perspective.getElements("goal");
                    if (gs != null) {
                        for (Element g : gs) {
                            goals.put(g.name, glScene.getVector(g.name));
                        }
                    }
                    final Map<String, Vector> spheres = new HashMap<>();
                    List<Element> ss = perspective.getElements("sphere");
                    if (ss != null) {
                        for (Element s : ss) {
                            spheres.put(s.name, glScene.getVector(s.name));
                        }
                    }
                    node.setAnimation(new LaunchAnimation(perspective.size, perspective.inverseRotation, perspective.up, blocks, goals, perspective.linkedPortals, spheres) {
                        @Override
                        public void onBlockHit(String asteroid) {
                            vibrate(LANDING_VIBRATION);
                            sound(LANDING_SOUND, 0);
                        }

                        @Override
                        public void onPortalTraversed() {
                            vibrate(PORTAL_VIBRATION);
                            sound(PORTAL_SOUND, 0);
                        }

                        @Override
                        public void onComplete() {
                            boolean gameLost = false;
                            boolean gameWon = true;
                            for (Entry<String, Vector> s : spheres.entrySet()) {
                                String k = s.getKey();
                                Vector v = s.getValue();
                                if (PerspectiveUtils.isOutOfBounds(v, perspective.size)) {
                                    // if any spheres are out of bounds - game over
                                    gameLost = true;
                                } else if (!goals.containsValue(v)) {
                                    // if all spheres are in the goals - game won
                                    gameWon = false;
                                }
                                System.out.println("Move: " + k + " " + v);
                                perspective.solution.addMove(Move.newBuilder()
                                        .setKey(k)
                                        .setValue(PerspectiveUtils.vectorToLocation(v))
                                        .build());
                            }
                            if (gameLost) {
                                perspective.gameOver = true;
                                perspective.gameWon = false;
                                onGameLost();
                            } else if (gameWon) {
                                perspective.gameOver = true;
                                perspective.gameWon = true;
                                onGameWon();
                            } else {
                                checkDialogs();
                            }
                        }
                    });
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    private void checkDialogs() {
        List<String> dialogs = new ArrayList<>();
        List<Element> ss = perspective.getElements("sphere");
        if (ss != null) {
            for (Element s : ss) {
                Vector location = glScene.getVector(s.name);
                for (String d : perspective.dialogs.keySet()) {
                    if (location.equals(glScene.getVector(d))) {
                        dialogs.add(d);
                    }
                }
            }
        }
        if (dialogs.size() > 0) {
            Collections.sort(dialogs, new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return extractInt(o1) - extractInt(o2);
                }

                int extractInt(String s) {
                    String num = s.replaceAll("\\D", "");
                    // return 0 if no digits found
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
            onDialog(dialogs);
        }
    }

    private void onDialog(final List<String> dialogs) {
        System.out.println("onDialog: " + dialogs);
        runOnUiThread(new Runnable() {
            int index = 0;
            CardView card;
            TextView author;
            TextView content;
            Button next;

            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameDialogTheme);
                card = (CardView) getLayoutInflater().inflate(R.layout.dialog_game_dialog, null);
                author = card.findViewById(R.id.game_dialog_author);
                content = card.findViewById(R.id.game_dialog_content);
                next = card.findViewById(R.id.game_dialog_next);
                builder.setView(card);
                builder.setCancelable(false);
                gameDialogDialog = builder.create();
                next.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        index++;
                        if (index < dialogs.size()) {
                            setupDialog();
                        } else {
                            gameDialogDialog.dismiss();
                        }
                    }
                });
                setupDialog();
                gameDialogDialog.show();
            }

            private void setupDialog() {
                String n = dialogs.get(index);
                Dialog d = perspective.dialogs.get(n);
                if ("journal".equals(d.getType())) {
                    sound(JOURNAL_SOUND, 0);
                }
                int fg = colourStringToInt(d.getForegroundColour());
                int bg = colourStringToInt(d.getBackgroundColour());
                String a = d.getAuthor();
                String c = d.getContent();
                List<String> es = d.getElementList();// TODO

                card.setCardBackgroundColor(bg);
                String separator = System.getProperty("line.separator");
                author.setText(a.replace("\\n", separator));
                author.setTextColor(fg);
                content.setText(c.replace("\\n", separator));
                content.setTextColor(fg);
            }
        });
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
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameDialogTheme);
                View layout = getLayoutInflater().inflate(R.layout.dialog_game_lost, null);
                Button main = layout.findViewById(R.id.game_lost_main_menu);
                Button retry = layout.findViewById(R.id.game_lost_retry);
                builder.setView(layout);
                builder.setCancelable(false);
                gameOverDialog = builder.create();
                main.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gameOverDialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                retry.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gameOverDialog.dismiss();
                        perspective.clearAllLocations();
                        loadPuzzle();
                    }
                });
                gameOverDialog.show();
            }
        });
    }

    @Override
    public void onGameWon() {
        Log.d(PerspectiveUtils.TAG, "Game Won");
        if (puzzleIndex < world.getPuzzleCount()) {
            sound(GOAL_LEVEL_SOUND, 0);
        } else {
            sound(GOAL_WORLD_SOUND, 0);
        }
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
            sound(STAR_SOUND, stars - 1);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameDialogTheme);
                View layout = getLayoutInflater().inflate(R.layout.dialog_game_won, null);
                layout.findViewById(R.id.game_won_star1).setVisibility((stars > 0) ? View.VISIBLE : View.GONE);
                layout.findViewById(R.id.game_won_star2).setVisibility((stars > 1) ? View.VISIBLE : View.GONE);
                layout.findViewById(R.id.game_won_star3).setVisibility((stars > 2) ? View.VISIBLE : View.GONE);
                layout.findViewById(R.id.game_won_star4).setVisibility((stars > 3) ? View.VISIBLE : View.GONE);
                layout.findViewById(R.id.game_won_star5).setVisibility((stars > 4) ? View.VISIBLE : View.GONE);
                Button main = layout.findViewById(R.id.game_won_main_menu);
                CardView retryCard = layout.findViewById(R.id.game_won_retry_card);
                Button retry = layout.findViewById(R.id.game_won_retry);
                Button next = layout.findViewById(R.id.game_won_next);
                builder.setView(layout);
                builder.setCancelable(false);
                gameOverDialog = builder.create();
                main.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gameOverDialog.dismiss();
                        setResult(RESULT_OK);
                        finish();
                    }
                });
                if (stars < 5) {
                    retry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            gameOverDialog.dismiss();
                            perspective.clearAllLocations();
                            loadPuzzle();
                        }
                    });
                    retryCard.setVisibility(View.VISIBLE);
                } else {
                    retryCard.setVisibility(View.GONE);
                }

                if (puzzleIndex < world.getPuzzleCount()) {
                    next.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            gameOverDialog.dismiss();
                            perspective.clearAllLocations();
                            puzzleIndex++;
                            loadPuzzle();
                        }
                    });
                } else {
                    final String nextWorld = getNextWorld();
                    if (!nextWorld.equals(PerspectiveUtils.WORLD_TUTORIAL)) {
                        next.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                gameOverDialog.dismiss();
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
                gameOverDialog.show();
            }
        });
    }

    public void onGameMenu() {
        sound(BUTTON_SOUND, 0);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this, R.style.GameDialogTheme);
                View layout = getLayoutInflater().inflate(R.layout.dialog_game_menu, null);
                builder.setView(layout);
                gameMenuDialog = builder.create();
                Button buttonMain = layout.findViewById(R.id.game_menu_main_menu);
                buttonMain.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gameMenuDialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
                Button buttonSettings = layout.findViewById(R.id.game_menu_settings);
                buttonSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gameMenuDialog.dismiss();
                        Intent intent = new Intent(GameActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }
                });
                Button buttonReset = layout.findViewById(R.id.game_menu_reset);
                buttonReset.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gameMenuDialog.dismiss();
                        perspective.clearAllLocations();
                        loadPuzzle();
                    }
                });
                gameMenuDialog.show();
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
