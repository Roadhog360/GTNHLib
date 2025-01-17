package com.gtnewhorizon.gtnhlib.client.model.template;

import net.minecraft.util.ResourceLocation;

import com.gtnewhorizon.gtnhlib.client.model.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.ModelVariant;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;

public class Model6Rot {

    /**
     * Use this to create JSON model rotatable in 6 directions, relative to ForgeDirection 0 = DOWN 1 = UP 2 = NORTH 3 =
     * SOUTH 4 = WEST* 5 = EAST* * WEST and EAST are flipped in some places but not others. Truth be told, I have no
     * idea what the hell the "correct" way actually is...
     */

    public final QuadProvider[] models = new QuadProvider[6];
    private final ModelVariant[] modelIds;

    public Model6Rot(ResourceLocation modelLoc) {
        this.modelIds = new ModelVariant[] { new ModelVariant(modelLoc, 0, 180, false),
                new ModelVariant(modelLoc, 0, 0, false),
                // TODO: Check if these rotations are correct
                new ModelVariant(modelLoc, 0, 90, false),
                // TODO: For some reason the below rotations act exactly as the above one. Why don't they "listen" to
                // the X rotation?
                new ModelVariant(modelLoc, 180, 90, false), new ModelVariant(modelLoc, 90, 90, false),
                new ModelVariant(modelLoc, 270, 90, false) };

        ModelLoader.registerModels(() -> loadModels(this), this.modelIds);
    }

    public static void loadModels(Model6Rot model) {
        for (int i = 0; i < 6; ++i) model.models[i] = ModelLoader.getModel(model.modelIds[i]);
    }
}
