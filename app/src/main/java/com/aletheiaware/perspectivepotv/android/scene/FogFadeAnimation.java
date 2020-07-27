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

import com.aletheiaware.joy.scene.Animation;
import com.aletheiaware.joy.utils.JoyUtils;

public class FogFadeAnimation extends Animation {

    private static final float FADE_DURATION = 1.5f;// 1.5 second fade

    private final float[] destination;
    private final float startingIntensity;
    private final float endingIntensity;
    private long start = -1;

    public FogFadeAnimation(float[] destination, float startingIntensity, float endingIntensity) {
        // System.out.println("FogFadeAnimation: " + startingIntensity + " to " + endingIntensity);
        this.destination = destination;
        this.startingIntensity = startingIntensity;
        this.endingIntensity = endingIntensity;
    }

    public void setStart(long start) {
        this.start = start;
    }

    @Override
    public boolean tick() {
        long now = System.currentTimeMillis();
        if (start < 0) {
            start = now;
            onBegin();
        }

        float progress = (now - start) / 1000.0f;// Time to seconds
        // System.out.println("Time: " + progress);

        destination[0] = JoyUtils.map(progress, 0, FADE_DURATION, startingIntensity, endingIntensity);
        if (destination[0] < endingIntensity) {
            destination[0] = endingIntensity;
        }
        // System.out.println("Fog: " + destination[0]);

        return progress >= FADE_DURATION;
    }
}
