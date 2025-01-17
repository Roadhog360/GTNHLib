package com.gtnewhorizon.gtnhlib.client.model.isbrh;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.google.common.annotations.Beta;
import com.gtnewhorizon.gtnhlib.client.model.json.JsonModel;
import com.gtnewhorizon.gtnhlib.client.model.json.ModelDisplay;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;
import com.gtnewhorizon.gtnhlib.client.renderer.util.MathUtil;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/// @author Roadhog360
@Beta
public interface JsonModelISBRH extends ISimpleBlockRenderingHandler {

    QuadProvider getInventoryModel(Block block, int meta);

    QuadProvider getWorldModel(IBlockAccess world, int x, int y, int z, int meta);

    void doWorldRender(QuadProvider model, @NotNull IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer, int meta);

    void doInventoryRender(QuadProvider model, Block block, int meta, int modelID, RenderBlocks renderer);

    @Override
    default boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        int meta = world.getBlockMetadata(x, y, z);
        doWorldRender(getWorldModel(world, x, y, z, meta), world, x, y, z, block, modelId, renderer, meta);
        return true;
    }

    /// Used in the inventory, called by {@link #getOverrideIcon(QuadProvider, ForgeDirection, IBlockAccess, int, int,
    /// int)} if that is not overridden.
    @Nullable
    default IIcon getOverrideIcon(QuadProvider model, ForgeDirection side) {
        return null;
    }

    @Nullable
    default IIcon getOverrideIcon(QuadProvider model, ForgeDirection side, IBlockAccess world, int x, int y, int z) {
        return getOverrideIcon(model, side);
    }

    default void renderInventoryBlock(Block block, int meta, int modelID, RenderBlocks renderBlocks,
            @Nullable ModelDisplay.Position context) {
        final Tessellator tessellator = Tessellator.instance;
        if (block.getRenderBlockPass() == 1) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_BLEND);
        }
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        QuadProvider modelToRender = getInventoryModel(block, meta);

        ModelDisplay displayInfo = null;
        if (context != null && modelToRender instanceof JsonModel jsonModel) {
            displayInfo = jsonModel.getDisplay().get(context);
            Vector3f rotation = displayInfo.getRotation();
            Vector3f translation = displayInfo.getTranslation();
            Vector3f scale = displayInfo.getScale();

            if (!MathUtil.fuzzy_eq(rotation.x(), 0) || !MathUtil.fuzzy_eq(rotation.y(), 0)
                    || !MathUtil.fuzzy_eq(rotation.z(), 0)) {
                GL11.glRotatef(rotation.x(), 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(rotation.y(), 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(rotation.z(), 0.0F, 0.0F, 1.0F);
            }
            if (!MathUtil.fuzzy_eq(translation.x(), 0) || !MathUtil.fuzzy_eq(translation.y(), 0)
                    || !MathUtil.fuzzy_eq(translation.z(), 0)) {
                GL11.glTranslatef(translation.x(), translation.y(), translation.z());
            }
            if (!MathUtil.fuzzy_eq(scale.x(), 1) || !MathUtil.fuzzy_eq(scale.y(), 1)
                    || !MathUtil.fuzzy_eq(scale.z(), 1)) {
                GL11.glScalef(scale.x(), scale.y(), scale.z());
            }
        }

        tessellator.startDrawingQuads();
        doInventoryRender(modelToRender, block, meta, modelID, renderBlocks);
        tessellator.draw();

        if (displayInfo != null) {
            Vector3f rotation = displayInfo.getRotation();
            Vector3f translation = displayInfo.getTranslation();
            Vector3f scale = displayInfo.getScale();

            if (!MathUtil.fuzzy_eq(rotation.x(), 0) || !MathUtil.fuzzy_eq(rotation.y(), 0)
                    || !MathUtil.fuzzy_eq(rotation.z(), 0)) {
                GL11.glRotatef(rotation.x(), -1.0F, 0.0F, 0.0F);
                GL11.glRotatef(rotation.y(), 0.0F, -1.0F, 0.0F);
                GL11.glRotatef(rotation.z(), 0.0F, 0.0F, -1.0F);
            }
            if (!MathUtil.fuzzy_eq(translation.x(), 0) || !MathUtil.fuzzy_eq(translation.y(), 0)
                    || !MathUtil.fuzzy_eq(translation.z(), 0)) {
                GL11.glTranslatef(-translation.x(), -translation.y(), -translation.z());
            }
            if (!MathUtil.fuzzy_eq(scale.x(), 1) || !MathUtil.fuzzy_eq(scale.y(), 1)
                    || !MathUtil.fuzzy_eq(scale.z(), 1)) {
                GL11.glScalef(-scale.x(), -scale.y(), -scale.z());
            }
        }

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    default void renderInventoryBlock(Block block, int meta, int modelID, RenderBlocks renderer) {
        renderInventoryBlock(block, meta, modelID, renderer, null);
    }
}
