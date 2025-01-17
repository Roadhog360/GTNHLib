package com.gtnewhorizon.gtnhlib.client.model.isbrh;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import com.gtnewhorizon.gtnhlib.client.model.ModelLoader;
import com.gtnewhorizon.gtnhlib.client.model.ModelVariant;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;
import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

/// @author Roadhog360
@ThreadSafeISBRH(perThread = false)
public class RenderJSONBasic extends RenderJSONBase {

    protected QuadProvider modelBlock;
    protected QuadProvider modelItem;

    public RenderJSONBasic(@Nullable String itemLoc, String blockLoc) {
        if (itemLoc != null) {
            ModelVariant var = new ModelVariant(new ResourceLocation(itemLoc + ".json"), 0, 0, false);
            ModelLoader.registerModels(() -> modelItem = ModelLoader.getModel(var), var);
        }
        ModelVariant var = new ModelVariant(new ResourceLocation(blockLoc + ".json"), 0, 0, false);
        ModelLoader.registerModels(() -> modelBlock = ModelLoader.getModel(var), var);

    }

    public QuadProvider getInventoryModel(Block block, int meta) {
        return modelItem;
    }

    public QuadProvider getWorldModel(IBlockAccess world, int x, int y, int z, int meta) {
        return modelBlock;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return modelItem != null;
    }
}
