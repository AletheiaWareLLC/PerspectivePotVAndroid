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

package com.aletheiaware.perspectivepotv.android.scene;

import android.opengl.GLES20;

import com.aletheiaware.joy.android.scene.GLProgram;
import com.aletheiaware.joy.android.scene.GLScene;
import com.aletheiaware.joy.android.scene.GLUtils;
import com.aletheiaware.joy.scene.Scene;
import com.aletheiaware.joy.scene.SceneGraphNode;

public class BlastNode extends SceneGraphNode {

    private final String programName;

    public BlastNode(String programName) {
        super();
        this.programName = programName;
    }

    @Override
    public void draw(Scene scene) {
        int[] enabled = scene.getIntArray("blast-enabled");
        if (enabled != null && enabled.length > 0 && enabled[0] != 0) {
            super.draw(scene);
        }
    }

    @Override
    public void before(Scene scene) {
        // Try set u_BlastRandom
        try {
            GLProgram program = ((GLScene) scene).getProgramNode(programName).getProgram();
            int blastRandomHandle = program.getUniformLocation("u_BlastRandom");
            float[] blastRandom = scene.getFloatArray("blast-random");
            // Pass in the blast information
            GLES20.glUniform1f(blastRandomHandle, blastRandom[0]);
        } catch (Exception e) {
            // Ignored
        }
        GLUtils.checkError("BlastNode.before");
    }

    @Override
    public void after(Scene scene) {
        GLUtils.checkError("BlastNode.after");
    }
}
