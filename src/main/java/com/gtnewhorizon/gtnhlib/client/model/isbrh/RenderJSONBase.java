package com.gtnewhorizon.gtnhlib.client.model.isbrh;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.client.model.json.JsonModel;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.Quad;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadProvider;
import com.gtnewhorizon.gtnhlib.client.renderer.quad.QuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.util.DirectionUtil;
import com.gtnewhorizon.gtnhlib.util.ObjectPooler;

import cpw.mods.fml.client.registry.RenderingRegistry;

/// @author Roadhog360
/// @author Omni (AKA aptugongermatherinn, ah-OOG-ah)
public abstract class RenderJSONBase implements JsonModelISBRH {

    protected final ThreadLocal<ObjectPooler<Quad>> quadPool = ThreadLocal
            .withInitial(() -> new ObjectPooler<>(Quad::new));
    protected static final Random modelRand = new Random();

    private final int modelID;

    private final ThreadLocal<AOHelper> lightingHelper = ThreadLocal.withInitial(AOHelper::new);

    public RenderJSONBase() {
        modelID = RenderingRegistry.getNextAvailableRenderId();
    }

    public RenderJSONBase(int modelID) {
        this.modelID = modelID;
    }

    /// Override this to add more render steps, and call the super again passing another {@link QuadProvider}, if you
    /// want to render more than one model at once.
    /// The model param is the one passed in by {@link #renderWorldBlock(IBlockAccess, int, int, int, Block, int,
    /// RenderBlocks)}
    /// TODO: Solid blocks appear really dark for some reason.
    @Override
    public void doWorldRender(QuadProvider model, @NotNull IBlockAccess world, int x, int y, int z, Block block,
            int modelId, RenderBlocks renderer, int meta) {
        Random random = world instanceof World worldIn ? worldIn.rand : modelRand;
        Tessellator tesselator = Tessellator.instance;
        // I forget if JSON models are dynamic, you can hardcode this since they're all the same.
        // You only need the check for a general renderer that doesn't know what kind of QuadProvider it's getting
        final Supplier<QuadView> sq = model.isDynamic() ? quadPool.get()::getInstance : null;

        int color = model.getColor(world, x, y, z, block, meta, random);

        tesselator.setBrightness(
                world instanceof World worldIn
                        ? worldIn.getBlockLightValue_do(x, y, z, block.getUseNeighborBrightness())
                        : block.getMixedBrightnessForBlock(world, x, y, z));

        // ALL_DIRECTIONS comes from NHLib too - caches .values() to avoid allocating a bunch
        for (ForgeDirection dir : DirectionUtil.ALL_DIRECTIONS) {
            // Saves a little performance if you cull faces ASAP, although you'd have to write this yourself
            if (dir != ForgeDirection.UNKNOWN && this.isCulled(world, x, y, z, block, meta, dir)) continue;

            // iterates over the quads and dumps em into the tesselator, nothing special
            for (final QuadView quad : model.getQuads(world, x, y, z, block, meta, dir, random, color, sq)) {

                ForgeDirection renderDir = quad.getLightFace();

                if (quad.getColorIndex() != -1 && color == -1) {
                    color = block.colorMultiplier(world, x, y, z);
                }

                final int r = color & 255;
                final int g = color >> 8 & 255;
                final int b = color >> 16 & 255;

                if (Minecraft.isAmbientOcclusionEnabled()
                        && (!(model instanceof JsonModel jsonModel) || jsonModel.isUseAO())) {
                    AOHelper aoHelper = lightingHelper.get();
                    if (!quad.isShade()) {
                        aoHelper.setLightnessOverride(1);
                    }
                    aoHelper.setRenderBlocks(renderer);

                    if (dir == ForgeDirection.UNKNOWN) {
                        renderer.setRenderBounds(0.1F, 0.1F, 0.1F, 0.9F, 0.9F, 0.9F);
                    } else {
                        renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
                    }

                    aoHelper.setupLighting(
                            block,
                            MathHelper.floor_double(x),
                            MathHelper.floor_double(y),
                            MathHelper.floor_double(z),
                            renderDir).setupColor(renderDir, color);
                    aoHelper.clearLightnessOverride();

                    renderQuadAO(
                            model,
                            quad,
                            x,
                            y,
                            z,
                            tesselator,
                            renderer,
                            renderDir,
                            renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture
                                    : getOverrideIcon(model, dir, world, x, y, z));
                } else {
                    if (quad.isShade()) {
                        float dirShading = AOHelper.getDirShading(renderDir);
                        tesselator
                                .setColorOpaque((int) (r * dirShading), (int) (g * dirShading), (int) (b * dirShading));
                    } else {
                        tesselator.setColorOpaque(r, g, b);
                    }
                    renderQuad(
                            model,
                            quad,
                            x,
                            y,
                            z,
                            tesselator,
                            renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture
                                    : getOverrideIcon(model, renderDir, world, x, y, z));
                }
            }
        }
    }

    protected final void renderQuadAO(QuadProvider model, QuadView quad, float x, float y, float z,
            Tessellator tessellator, RenderBlocks renderer, ForgeDirection quadFacing, @Nullable IIcon overrideIcon) {
        // DOWN, SOUTH = dir + 0;
        // EAST = dir + 1
        // UP = dir + 2
        // NORTH, WEST = dir + 3
        int offsetAO = switch (quadFacing) {
            case DOWN, SOUTH, UNKNOWN -> 0;
            case NORTH, WEST -> 1;
            case UP -> 2;
            case EAST -> 3;
        };
        offsetAO += getUVRotation(quad);
        for (int i = 0; i < 4; ++i) {
            switch ((i + offsetAO) & 3) {
                case 0 -> topLeftAOSetup(tessellator, renderer);
                case 1 -> bottomLeftAOSetup(tessellator, renderer);
                case 2 -> bottomRightAOSetup(tessellator, renderer);
                case 3 -> topRightAOSetup(tessellator, renderer);
            }
            tessellator.addVertexWithUV(
                    getRenderX(x, quad, model, i),
                    getRenderY(y, quad, model, i),
                    getRenderZ(z, quad, model, i),
                    getU(quad, overrideIcon, i),
                    getV(quad, overrideIcon, i));
        }
    }

    // 0 for not rotated, 1 for 90, 2 for 180, and 3 for 270.
    protected int getUVRotation(QuadView quad) {
        return 0;
    }

    protected void topLeftAOSetup(Tessellator tessellator, RenderBlocks renderer) {
        tessellator.setColorOpaque_F(renderer.colorRedTopLeft, renderer.colorGreenTopLeft, renderer.colorBlueTopLeft);
        tessellator.setBrightness(renderer.brightnessTopLeft);
    }

    protected void bottomLeftAOSetup(Tessellator tessellator, RenderBlocks renderer) {
        tessellator.setColorOpaque_F(
                renderer.colorRedBottomLeft,
                renderer.colorGreenBottomLeft,
                renderer.colorBlueBottomLeft);
        tessellator.setBrightness(renderer.brightnessBottomLeft);
    }

    protected void topRightAOSetup(Tessellator tessellator, RenderBlocks renderer) {
        tessellator
                .setColorOpaque_F(renderer.colorRedTopRight, renderer.colorGreenTopRight, renderer.colorBlueTopRight);
        tessellator.setBrightness(renderer.brightnessTopRight);
    }

    protected void bottomRightAOSetup(Tessellator tessellator, RenderBlocks renderer) {
        tessellator.setColorOpaque_F(
                renderer.colorRedBottomRight,
                renderer.colorGreenBottomRight,
                renderer.colorBlueBottomRight);
        tessellator.setBrightness(renderer.brightnessBottomRight);
    }

    protected void renderQuad(QuadProvider model, QuadView quad, float x, float y, float z, Tessellator tessellator,
            @Nullable IIcon overrideIcon) {
        for (int i = 0; i < 4; ++i) {
            tessellator.addVertexWithUV(
                    getRenderX(x, quad, model, i),
                    getRenderY(y, quad, model, i),
                    getRenderZ(z, quad, model, i),
                    getU(quad, overrideIcon, i),
                    getV(quad, overrideIcon, i));
        }
    }

    /// Override this to add more render steps, and call the super again passing another {@link QuadProvider}, if you
    /// want to render more than one model at once.
    /// The model param is the one passed in by {@link #renderInventoryBlock}
    @Override
    public void doInventoryRender(QuadProvider model, Block block, int meta, int modelID, RenderBlocks renderer) {
        Random random = modelRand;
        Tessellator tesselator = Tessellator.instance;
        // I forget if JSON models are dynamic, you can hardcode this since they're all the same.
        // You only need the check for a general renderer that doesn't know what kind of QuadProvider it's getting
        final Supplier<QuadView> sq = model.isDynamic() ? quadPool.get()::getInstance : null;

        int color = -1;

        for (ForgeDirection dir : DirectionUtil.ALL_DIRECTIONS) {
            // iterates over the quads and dumps em into the tesselator, nothing special
            for (final QuadView quad : model.getQuads(null, 0, 0, 0, block, meta, dir, random, color, sq)) {

                ForgeDirection renderDir = quad.getLightFace();

                tesselator.setNormal(renderDir.offsetX, renderDir.offsetY, renderDir.offsetZ);

                if (quad.getColorIndex() != -1) {
                    color = block.getRenderColor(meta);
                }

                final int r = color & 255;
                final int g = color >> 8 & 255;
                final int b = color >> 16 & 255;

                tesselator.setColorOpaque(r, g, b);

                renderQuad(
                        model,
                        quad,
                        0,
                        0,
                        0,
                        tesselator,
                        renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture
                                : getOverrideIcon(model, renderDir));
            }
        }
    }

    protected float getRenderX(float x, QuadView quad, QuadProvider model, int idx) {
        return quad.getX(idx) + x;
    }

    protected float getRenderY(float y, QuadView quad, QuadProvider model, int idx) {
        return quad.getY(idx) + y;
    }

    protected float getRenderZ(float z, QuadView quad, QuadProvider model, int idx) {
        return quad.getZ(idx) + z;
    }

    /// Almost works, seems a little inaccurate, but I can't really figure out why?
    protected float getU(QuadView quad, IIcon icon, int idx) {
        if (icon != null) {
            boolean isLower = quad.getTexU(0) == quad.getTexU(1) ? idx == 0 || idx == 1 : idx == 0 || idx == 3;
            // I tried 0, 2, 0, 2 here, but it seems 0,1 0,2 works better for some strange reason?
            float relative = Math.max(quad.getTexU(0), quad.getTexU(1)) - Math.min(quad.getTexU(0), quad.getTexU(2));
            return isLower ? icon.getMinU() + relative : icon.getMaxU() - relative;
        }

        return quad.getTexU(idx);
    }

    /// Almost works, seems a little inaccurate, but I can't really figure out why?
    protected float getV(QuadView quad, IIcon icon, int idx) {
        if (icon != null) {
            boolean isLower = quad.getTexV(0) == quad.getTexV(1) ? idx == 0 || idx == 1 : idx == 0 || idx == 3;
            // I tried 0, 2, 0, 2 here, but it seems 0,1 0,2 works better for some strange reason?
            float relative = Math.max(quad.getTexV(0), quad.getTexV(1)) - Math.min(quad.getTexV(0), quad.getTexV(2));
            return isLower ? icon.getMinV() + relative : icon.getMaxV() - relative;
        }

        return quad.getTexV(idx);
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    protected boolean isCulled(IBlockAccess world, int x, int y, int z, Block block, int meta, ForgeDirection dir) {
        // return !block.shouldSideBeRendered(world, x, y, z, dir.ordinal());
        return false; // TODO: Previous didn't work, but why?
    }

    @Override
    public int getRenderId() {
        return modelID;
    }
}
