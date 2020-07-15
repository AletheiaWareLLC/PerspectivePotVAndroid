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

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.aletheiaware.joy.JoyProto.Mesh;
import com.aletheiaware.joy.JoyProto.Shader;
import com.aletheiaware.joy.android.scene.GLCameraNode;
import com.aletheiaware.joy.android.scene.GLColourAttribute;
import com.aletheiaware.joy.android.scene.GLFogNode;
import com.aletheiaware.joy.android.scene.GLLightNode;
import com.aletheiaware.joy.android.scene.GLMaterialAttribute;
import com.aletheiaware.joy.android.scene.GLProgram;
import com.aletheiaware.joy.android.scene.GLProgramNode;
import com.aletheiaware.joy.android.scene.GLScene;
import com.aletheiaware.joy.android.scene.GLTextureAttribute;
import com.aletheiaware.joy.android.scene.GLUtils;
import com.aletheiaware.joy.android.scene.GLVertexNormalTextureMesh;
import com.aletheiaware.joy.android.scene.GLVertexNormalTextureMeshNode;
import com.aletheiaware.joy.scene.Animation;
import com.aletheiaware.joy.scene.Attribute;
import com.aletheiaware.joy.scene.AttributeNode;
import com.aletheiaware.joy.scene.Matrix;
import com.aletheiaware.joy.scene.MatrixTransformationNode;
import com.aletheiaware.joy.scene.MeshLoader;
import com.aletheiaware.joy.scene.ScaleNode;
import com.aletheiaware.joy.scene.TranslateNode;
import com.aletheiaware.joy.scene.Vector;
import com.aletheiaware.perspective.utils.PerspectiveUtils;
import com.aletheiaware.perspectivepotv.android.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class DebugActivity extends AppCompatActivity {

    private final float[] frustum = new float[2];
    private final float[] light = new float[4];
    private final int[] fogEnabled = new int[1];
    private final float[] fogIntensity = new float[1];
    private final Matrix model = new Matrix();
    private final Matrix view = new Matrix();
    private final Matrix projection = new Matrix();
    private final Matrix mv = new Matrix();
    private final Matrix mvp = new Matrix();
    private final Matrix mainRotation = new Matrix();
    private final Matrix inverseRotation = new Matrix();
    private final Matrix tempRotation = new Matrix();
    private final Vector cameraEye = new Vector();
    private final Vector cameraLookAt = new Vector();
    private final Vector cameraUp = new Vector();
    private final String program = "debug";

    private MediaPlayer mediaPlayer;
    private SoundPool soundPool;
    private Map<String, Integer> soundMap = new HashMap<>();
    private String musicName;
    private String soundName;
    private String meshName;
    private String materialName;
    private String textureName;
    private DebugView debugView;
    private GLScene scene;
    private MatrixTransformationNode rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        ImageButton menu = findViewById(R.id.debug_menu);
        final ScrollView options = findViewById(R.id.debug_options);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Menu Clicked");
                switch (options.getVisibility()) {
                    case View.VISIBLE:
                        System.out.println("Hiding Options");
                        options.setVisibility(View.GONE);
                        break;
                    case View.INVISIBLE:
                        // fallthrough
                    case View.GONE:
                        // fallthrough
                    default:
                        System.out.println("Showing Options");
                        options.setVisibility(View.VISIBLE);
                }
            }
        });
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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

        Spinner musicSpinner = findViewById(R.id.debug_music_spinner);
        final ArrayAdapter<CharSequence> musicAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        musicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        musicSpinner.setAdapter(musicAdapter);
        musicSpinner.setSelection(-1);
        musicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                musicName = musicAdapter.getItem(position) + "";
                updateMusic();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO
            }
        });

        Spinner soundSpinner = findViewById(R.id.debug_sound_spinner);
        final ArrayAdapter<CharSequence> soundAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        soundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        soundSpinner.setAdapter(soundAdapter);
        soundSpinner.setSelection(-1);
        soundSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                soundName = soundAdapter.getItem(position) + "";
                updateSound();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO
            }
        });

        Spinner meshSpinner = findViewById(R.id.debug_mesh_spinner);
        final ArrayAdapter<CharSequence> meshAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        meshAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        meshSpinner.setAdapter(meshAdapter);
        meshSpinner.setSelection(-1);
        meshSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                meshName = meshAdapter.getItem(position) + "";
                updateSight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO
            }
        });

        Spinner textureSpinner = findViewById(R.id.debug_texture_spinner);
        final ArrayAdapter<CharSequence> textureAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        textureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textureSpinner.setAdapter(textureAdapter);
        textureSpinner.setSelection(-1);
        textureSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textureName = textureAdapter.getItem(position) + "";
                updateSight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO
            }
        });

        Spinner materialSpinner = findViewById(R.id.debug_material_spinner);
        final ArrayAdapter<CharSequence> materialAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        materialAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        materialSpinner.setAdapter(materialAdapter);
        materialSpinner.setSelection(-1);
        materialSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                materialName = materialAdapter.getItem(position) + "";
                updateSight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO
            }
        });

        SeekBar fogDial = findViewById(R.id.debug_fog_dial);
        fogDial.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                fogEnabled[0] = i;
                fogIntensity[0] = i/100f;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        debugView = findViewById(R.id.debug_view);
        scene = new GLScene();
        debugView.setScene(scene);

        new Thread() {
            @Override
            public void run() {
                final AssetManager assets = getAssets();
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
                try {
                    String[] musics = assets.list("music/");
                    if (musics != null) {
                        for (String s : musics) {
                            System.out.println("Music Name: " + s);
                            musicAdapter.add(s);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    String[] sounds = assets.list("sound/");
                    if (sounds != null) {
                        for (String s : sounds) {
                            System.out.println("Sound Name: " + s);
                            try (AssetFileDescriptor fd = assets.openFd("sound/" + s)){
                                int id = soundPool.load(fd, 1);
                                System.out.println("Sound ID: " + id);
                                soundMap.put(s, id);
                                soundAdapter.add(s);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    String[] meshes = assets.list("mesh/");
                    if (meshes != null) {
                        for (String m : meshes) {
                            System.out.println("Mesh Name: " + m);
                            meshAdapter.add(m);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    String[] textures = assets.list("texture/");
                    if (textures != null) {
                        for (String t : textures) {
                            System.out.println("Texture Name: " + t);
                            textureAdapter.add(t);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                float size = 1.0f;
                float distance = 1.5f;
                System.out.println("Distance: " + distance);

                // Colours
                for (int i = 0; i < PerspectiveUtils.COLOUR_NAMES.length; i++) {
                    scene.putFloatArray(PerspectiveUtils.COLOUR_NAMES[i], PerspectiveUtils.COLOURS[i]);
                }
                for (int i = 0; i < PerspectiveUtils.MATERIAL_NAMES.length; i++) {
                    scene.putFloatArray(PerspectiveUtils.MATERIAL_NAMES[i], PerspectiveUtils.MATERIALS[i]);
                    materialAdapter.add(PerspectiveUtils.MATERIAL_NAMES[i]);
                }
                scene.putFloatArray(GLScene.BACKGROUND, PerspectiveUtils.PURPLE);
                // Frustum
                // Crop the scene proportionally
                frustum[0] = size * 0.5f;
                frustum[1] = distance + size;
                scene.putFloatArray("frustum", frustum);
                // Light
                // Ensure light is always outside
                light[0] = 0;
                light[1] = 0;
                light[2] = size / 2f;
                light[3] = 1.0f;
                scene.putFloatArray("light", light);
                // MVP
                scene.putMatrix("model", model.makeIdentity());
                scene.putMatrix("view", view.makeIdentity());
                scene.putMatrix("projection", projection.makeIdentity());
                scene.putMatrix("model-view", mv.makeIdentity());
                scene.putMatrix("model-view-projection", mvp.makeIdentity());
                // Rotation
                scene.putMatrix("main-rotation", mainRotation.makeIdentity());
                scene.putMatrix("inverse-rotation", inverseRotation.makeIdentity());
                scene.putMatrix("temp-rotation", tempRotation.makeIdentity());
                // Translation
                scene.putVector("front", new Vector(0,0,1));
                scene.putVector("back", new Vector(0,0,-1));
                scene.putVector("left", new Vector(1,0,0));
                scene.putVector("right", new Vector(-1,0,0));
                // Scale
                scene.putVector("quarter", new Vector(0.25f,0.25f,0.25f));
                // Camera
                // Ensure camera is always outside
                cameraEye.set(0.0f, 0.0f, distance);
                // Looking at the center
                cameraLookAt.set(0.0f, 0.0f, 0.0f);
                // Head pointing up Y axis
                cameraUp.set(0.0f, 1.0f, 0.0f);
                scene.putVector("camera-eye", cameraEye);
                scene.putVector("camera-look-at", cameraLookAt);
                scene.putVector("camera-up", cameraUp);
                // Fog
                scene.putFloatArray("fog-colour", PerspectiveUtils.PURPLE);
                scene.putIntArray("fog-enabled", fogEnabled);
                scene.putFloatArray("fog-intensity", fogIntensity);

                GLProgram debugProgram = new GLProgram(Shader.newBuilder()
                        .setName(program)
                        .setVertexSource("#if __VERSION__ >= 130\n" +
                                "  #define attribute in\n" +
                                "  #define varying out\n" +
                                "#endif\n" +
                                "uniform mat4 u_MVMatrix;\n" +
                                "uniform mat4 u_MVPMatrix;\n" +
                                "attribute vec4 a_Position;\n" +
                                "attribute vec3 a_Normal;\n" +
                                "attribute vec2 a_TexCoord;\n" +
                                "varying vec3 v_Position;\n" +
                                "varying vec3 v_Normal;\n" +
                                "varying vec2 v_TexCoord;\n" +
                                "void main() {\n" +
                                "    v_Position = vec3(u_MVMatrix * a_Position);\n" +
                                "    vec3 norm = vec3(u_MVMatrix * vec4(a_Normal, 0.0));\n" +
                                "    v_Normal = norm / length(norm);\n" +
                                "    v_TexCoord = a_TexCoord;\n" +
                                "    gl_Position = u_MVPMatrix * a_Position;\n" +
                                "}")
                        .setFragmentSource("#if __VERSION__ >= 130\n" +
                                "  #define varying in\n" +
                                "  out vec4 mgl_FragColour;\n" +
                                "#else\n" +
                                "  #define mgl_FragColour gl_FragColor\n" +
                                "#endif\n" +
                                "#ifdef GL_ES\n" +
                                "  #define MEDIUMP mediump\n" +
                                "  precision MEDIUMP float;\n" +
                                "#else\n" +
                                "  #define MEDIUMP\n" +
                                "#endif\n" +
                                "uniform MEDIUMP vec3 u_CameraEye;\n" +
                                "uniform MEDIUMP vec4 u_Colour;\n" +
                                "uniform MEDIUMP vec4 u_FogColour;\n" +
                                "uniform bool u_FogEnabled;\n" +
                                "uniform MEDIUMP float u_FogIntensity;\n" +
                                "uniform MEDIUMP vec3 u_LightPos;\n" +
                                "uniform MEDIUMP vec3 u_Material;// {Ambient, Diffuse, Specular}\n" +
                                "uniform bool u_TextureEnabled;\n" +
                                "uniform sampler2D u_Texture;\n" +
                                "varying MEDIUMP vec3 v_Position;\n" +
                                "varying MEDIUMP vec3 v_Normal;\n" +
                                "varying MEDIUMP vec2 v_TexCoord;\n" +
                                "void main() {\n" +
                                "    float ambient = 0.3*u_Material.x;\n" +
                                "    vec3 lightDiff = u_LightPos - v_Position;\n" +
                                "    vec3 lightVector = normalize(lightDiff);\n" +
                                "    float diffuse = max(dot(v_Normal, lightVector), 0.0)*u_Material.y;\n" +
                                "    vec3 reflectVector = reflect(lightVector, v_Normal);\n" +
                                "    vec3 cameraDiff = v_Position - u_CameraEye;\n" +
                                "    vec3 cameraVector = normalize(cameraDiff);\n" +
                                "    float specular = max(dot(reflectVector, cameraVector), 0.0)*u_Material.z;\n" +
                                "    vec4 colour = u_Colour;\n" +
                                "    if (u_TextureEnabled) {\n" +
                                "        colour = texture2D(u_Texture, v_TexCoord);\n" +
                                "    }\n" +
                                "    colour = colour*ambient + colour*diffuse + colour*specular;\n" +
                                "    if (u_FogEnabled) {\n" +
                                "        float fog = clamp(gl_FragCoord.z*u_FogIntensity, 0.0, 1.0);\n" +
                                "        colour = colour*(1.0-fog) + u_FogColour*fog;\n" +
                                "    }\n" +
                                "    mgl_FragColour = colour;\n" +
                                "    mgl_FragColour.a = 1.0;\n" +
                                "}")
                        .addAttributes("a_Position")
                        .addAttributes("a_Normal")
                        .addAttributes("a_TexCoord")
                        .addUniforms("u_CameraEye")
                        .addUniforms("u_Colour")
                        .addUniforms("u_FogColour")
                        .addUniforms("u_FogEnabled")
                        .addUniforms("u_FogIntensity")
                        .addUniforms("u_LightPos")
                        .addUniforms("u_Material")
                        .addUniforms("u_MVMatrix")
                        .addUniforms("u_MVPMatrix")
                        .addUniforms("u_Texture")
                        .addUniforms("u_TextureEnabled")
                        .build());

                GLProgramNode programNode = new GLProgramNode(debugProgram);
                scene.putProgramNode(program, programNode);

                GLLightNode light = new GLLightNode(program, "light");
                programNode.addChild(light);

                GLCameraNode camera = new GLCameraNode(program);
                light.addChild(camera);

                GLFogNode fog = new GLFogNode(program);
                camera.addChild(fog);

                rotation = new MatrixTransformationNode("main-rotation");
                rotation.setAnimation(new Animation() {
                    @Override
                    public boolean tick() {
                        tempRotation.makeRotationAxis(0.01f, new Vector(1, 1, 1));
                        mainRotation.makeMultiplication(mainRotation, tempRotation);
                        return false;
                    }
                });
                fog.addChild(rotation);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
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

    private void updateMusic() {
        if (musicName == null || musicName.equals("")) {
            System.out.println("No music name");
        } else {
            System.out.println("Music Name: " + musicName);
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
                AssetFileDescriptor afd = getAssets().openFd("music/" + musicName);
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
    }

    private void updateSound() {
        if (soundName == null || soundName.equals("")) {
            System.out.println("No sound name");
        } else {
            System.out.println("Sound Name: " + soundName);
            Integer id = soundMap.get(soundName);
            System.out.println("Sound ID: " + id);
            if (id != null) {
                int result = soundPool.play(id, 1, 1, 1, 0, 1);
                System.out.println("Playing Sound Result: " + result);
            }
        }
    }

    private void updateSight() {
        rotation.clear();
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new GLColourAttribute(program, PerspectiveUtils.DEFAULT_FG_COLOUR));

        if (materialName == null || materialName.equals("")) {
            System.out.println("No material name");
        } else {
            attributes.add(new GLMaterialAttribute(program, materialName));
        }

        if (textureName == null || textureName.equals("")) {
            System.out.println("No texture name");
        } else {
            attributes.add(new GLTextureAttribute(program, textureName) {
                @Override
                public void load() {
                    try (InputStream in = getAssets().open("texture/" + textureName)) {
                        int[] texIds = GLUtils.loadTexture(in);
                        System.out.println("Loaded Texture: " + textureName + " as " + Arrays.toString(texIds));
                        scene.putIntArray(textureName, texIds);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        AttributeNode attributeNode = new AttributeNode(attributes.toArray(new Attribute[0]));
        rotation.addChild(attributeNode);

        if (meshName == null || meshName.equals("")) {
            System.out.println("No mesh name");
        } else {
            GLVertexNormalTextureMeshNode meshNode = new GLVertexNormalTextureMeshNode(program, meshName);
            attributeNode.addChild(meshNode);

            TranslateNode translateFrontNode = new TranslateNode("front");
            attributeNode.addChild(translateFrontNode);
            ScaleNode scaleFrontNode = new ScaleNode("quarter");
            translateFrontNode.addChild(scaleFrontNode);
            scaleFrontNode.addChild(meshNode);

            TranslateNode translateBackNode = new TranslateNode("back");
            attributeNode.addChild(translateBackNode);
            ScaleNode scaleBackNode = new ScaleNode("quarter");
            translateBackNode.addChild(scaleBackNode);
            scaleBackNode.addChild(meshNode);

            TranslateNode translateLeftNode = new TranslateNode("left");
            attributeNode.addChild(translateLeftNode);
            ScaleNode scaleLeftNode = new ScaleNode("quarter");
            translateLeftNode.addChild(scaleLeftNode);
            scaleLeftNode.addChild(meshNode);

            TranslateNode translateRightNode = new TranslateNode("right");
            attributeNode.addChild(translateRightNode);
            ScaleNode scaleRightNode = new ScaleNode("quarter");
            translateRightNode.addChild(scaleRightNode);
            scaleRightNode.addChild(meshNode);

            if (scene.getVertexNormalTextureMesh(meshName) == null) {
                try {
                    new MeshLoader(getAssets().open("mesh/" + meshName)) {
                        @Override
                        public void onMesh(Mesh mesh) throws IOException {
                            System.out.println("Loaded Mesh: " + mesh.getName());
                            scene.putVertexNormalTextureMesh(meshName, new GLVertexNormalTextureMesh(mesh));
                        }
                    }.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
