package com.gtnewhorizon.gtnhlib.client.model.isbrh;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.client.model.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.ModelVariant;
import com.gtnewhorizon.gtnhlib.client.model.template.Model6Rot;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

/// @author Roadhog360
@ThreadSafeISBRH(perThread = false)
public class RenderJSON6Rot extends RenderJSONBase {

    private QuadProvider itemModel;
    private final Model6Rot model;

    public RenderJSON6Rot(@Nullable String itemLoc, String blockLoc) {
        if (itemLoc != null) {
            ModelVariant var = new ModelVariant(new ResourceLocation(itemLoc + ".json"), 0, 0, false);
            ModelLoader.registerModels(() -> itemModel = ModelLoader.getModel(var), var);
        }
        model = new Model6Rot(new ResourceLocation(blockLoc + ".json"));
    }

    @Override
    public QuadProvider getInventoryModel(Block block, int meta) {
        return itemModel;
    }

    @Override
    public QuadProvider getWorldModel(IBlockAccess world, int x, int y, int z, int meta) {
        return model.models[meta % 6];
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return itemModel != null;
    }
}
