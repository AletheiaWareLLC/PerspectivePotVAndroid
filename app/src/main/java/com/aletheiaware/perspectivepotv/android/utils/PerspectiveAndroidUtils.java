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

package com.aletheiaware.perspectivepotv.android.utils;

import android.content.Context;
import android.content.res.AssetManager;

import com.aletheiaware.joy.JoyProto.Mesh;
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
import com.aletheiaware.joy.scene.Attribute;
import com.aletheiaware.joy.scene.AttributeNode;
import com.aletheiaware.joy.scene.MatrixTransformationNode;
import com.aletheiaware.joy.scene.MeshLoader;
import com.aletheiaware.joy.scene.SceneGraphNode;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.PerspectiveProto.World;
import com.aletheiaware.perspective.utils.PerspectiveUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.WorkerThread;

public class PerspectiveAndroidUtils {

    public static final String ORIENTATION_EXTRA = "orientation";
    public static final String OUTLINE_EXTRA = "outline";
    public static final String PUZZLE_EXTRA = "puzzle";
    public static final String WORLD_EXTRA = "world";

    private PerspectiveAndroidUtils() {
    }

    public static Map<String, MatrixTransformationNode> createSceneGraphs(GLScene scene, World world) {
        Map<String, MatrixTransformationNode> graphs = new HashMap<>();
        for (String shader : world.getShaderMap().keySet()) {
            GLProgram program = new GLProgram(world.getShaderOrThrow(shader));

            GLProgramNode programNode = new GLProgramNode(program);
            scene.putProgramNode(shader, programNode);

            GLLightNode light = new GLLightNode(shader, "light");
            programNode.addChild(light);

            GLCameraNode camera = new GLCameraNode(shader);
            light.addChild(camera);

            GLFogNode fog = new GLFogNode(shader);
            camera.addChild(fog);

            MatrixTransformationNode rotation = new MatrixTransformationNode("main-rotation");
            fog.addChild(rotation);

            graphs.put(shader, rotation);
        }

        return graphs;
    }

    public static World getWorld(AssetManager assets, String world) throws IOException {
        return PerspectiveUtils.readWorld(assets.open("world/" + world + ".pb"));
    }

    @WorkerThread
    public static void saveSolution(Context context, String world, String puzzle, Solution solution) throws IOException {
        PerspectiveUtils.saveSolution(context.getFilesDir(), world, puzzle, solution);
    }

    @WorkerThread
    public static Solution loadSolution(Context context, String world, String puzzle) throws IOException {
        return PerspectiveUtils.loadSolution(context.getFilesDir(), world, puzzle);
    }

    @WorkerThread
    public static void clearSolutions(Context context) throws IOException {
        PerspectiveUtils.clearSolutions(context.getFilesDir());
    }

    public static SceneGraphNode getSceneGraphNode(GLScene scene, AssetManager assets, String shader, String name, String type, String mesh, String colour, String texture, String material) throws IOException {
        switch (type) {
            case "sphere":
                // Ensure space ship always points up
                MatrixTransformationNode rotationNode = new MatrixTransformationNode("inverse-rotation");

                // Split by semi-colon for different parts of ship (Ship-body;Ship-canopy;Ship-blast)
                String[] meshes = mesh.split(";");
                String[] colours = colour.split(";");
                String[] textures = texture.split(";");
                String[] materials = material.split(";");

                // Add each part to rotationNode
                for (int i = 0; i < meshes.length && i < colours.length && i < textures.length && i < materials.length; i++) {
                    ensureMeshLoaded(scene, assets, meshes[i]);

                    List<Attribute> attributes = createAttributes(scene, assets, shader, colours[i], textures[i], materials[i]);
                    AttributeNode attributeNode = new AttributeNode(attributes.toArray(new Attribute[0]));
                    attributeNode.addChild(new GLVertexNormalTextureMeshNode(shader, meshes[i]));
                    rotationNode.addChild(attributeNode);
                }

                return rotationNode;
            case "outline":
            case "block":
            case "goal":
            case "portal":
            case "sky":
                ensureMeshLoaded(scene, assets, mesh);

                List<Attribute> attributes = createAttributes(scene, assets, shader, colour, texture, material);
                AttributeNode attributeNode = new AttributeNode(attributes.toArray(new Attribute[0]));
                attributeNode.addChild(new GLVertexNormalTextureMeshNode(shader, mesh));
                return attributeNode;
            default:
                System.err.println("Unrecognized: " + shader + " " + name + " " + type + " " + mesh + " " + colour + " " + texture + " " + material);
        }
        return null;
    }

    private static List<Attribute> createAttributes(final GLScene scene, final AssetManager assets, String shader, String colour, final String texture, String material) {
        List<Attribute> attributes = new ArrayList<>();
        if (colour != null && !colour.isEmpty()) {
            System.out.println("Creating Colour Attribute: " + colour);
            attributes.add(new GLColourAttribute(shader, colour));
        }
        if (texture != null && !texture.isEmpty()) {
            System.out.println("Creating Texture Attribute: " + texture);
            attributes.add(new GLTextureAttribute(shader, texture) {
                @Override
                public void load() {
                    try (InputStream in = assets.open("texture/" + texture)) {
                        int[] texIds = GLUtils.loadTexture(in);
                        System.out.println("Loaded Texture: " + texture + " as " + Arrays.toString(texIds));
                        scene.putIntArray(texture, texIds);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if (material != null && !material.isEmpty()) {
            System.out.println("Creating Material Attribute: " + material);
            attributes.add(new GLMaterialAttribute(shader, material));
        }
        return attributes;
    }

    private static void ensureMeshLoaded(final GLScene scene, final AssetManager assets, final String mesh) throws IOException {
        if (scene.getVertexNormalTextureMesh(mesh) == null) {
            new MeshLoader(assets.open("mesh/" + mesh)) {
                @Override
                public void onMesh(Mesh m) throws IOException {
                    System.out.println("Loaded Mesh: " + mesh);
                    scene.putVertexNormalTextureMesh(mesh, new GLVertexNormalTextureMesh(m));
                }
            }.start();
        }
    }

}
