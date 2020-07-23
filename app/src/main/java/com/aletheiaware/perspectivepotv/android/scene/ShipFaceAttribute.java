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

    public ShipFaceAttribute(String programName) {
        super(programName);
    }

    @Override
    public String getTextureName(Scene scene) {
        return SHIP_FACE[scene.getIntArray("ship-emotion")[0]];
    }
}
