package com.aletheiaware.perspectivepotv.android.scene;

import com.aletheiaware.joy.android.scene.GLTextureAttribute;
import com.aletheiaware.joy.scene.Scene;

public abstract class ShipFaceAttribute extends GLTextureAttribute {

    public static final int SHIP_FACE_HAPPY = 0;
    public static final int SHIP_FACE_SAD = 1;

    public static final String[] SHIP_FACE = {
            "Ship-canopy-happy.png",
            "Ship-canopy-sad.png",
    };

    public String textureName;

    public ShipFaceAttribute(String programName) {
        super(programName, "");
    }

    @Override
    public int[] getTexture(Scene scene) {
        textureName = SHIP_FACE[scene.getIntArray("ship-emotion")[0]];
        return scene.getIntArray(textureName);
    }
}
