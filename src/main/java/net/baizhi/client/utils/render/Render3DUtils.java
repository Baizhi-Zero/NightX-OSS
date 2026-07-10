package net.baizhi.client.utils.render;

import net.baizhi.client.utils.MinecraftInstance;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class Render3DUtils extends MinecraftInstance {

    public static void drawEntityBox(Entity entity, Color fillColor, Color outlineColor, float lineWidth) {
        if (entity == null) return;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        if (bb == null) return;

        double minX = bb.minX - x + mc.getRenderManager().renderPosX;
        double minY = bb.minY - y + mc.getRenderManager().renderPosY;
        double minZ = bb.minZ - z + mc.getRenderManager().renderPosZ;
        double maxX = bb.maxX - x + mc.getRenderManager().renderPosX;
        double maxY = bb.maxY - y + mc.getRenderManager().renderPosY;
        double maxZ = bb.maxZ - z + mc.getRenderManager().renderPosZ;

        drawFilledBox(minX, minY, minZ, maxX, maxY, maxZ, fillColor);
        drawOutlinedBox(minX, minY, minZ, maxX, maxY, maxZ, outlineColor, lineWidth);
    }

    public static void drawFilledBox(double minX, double minY, double minZ,
                                      double maxX, double maxY, double maxZ, Color color) {
        if (color.getAlpha() <= 0) return;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        renderer.pos(minX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        renderer.pos(minX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        renderer.pos(minX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        renderer.pos(minX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        renderer.pos(minX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(minX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        renderer.pos(maxX, minY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, maxY, minZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, maxY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();
        renderer.pos(maxX, minY, maxZ).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).endVertex();

        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    public static void drawOutlinedBox(double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ, Color color, float lineWidth) {
        if (color.getAlpha() <= 0) return;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        renderer.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, minY, minZ).color(r, g, b, a).endVertex();

        renderer.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();

        renderer.pos(minX, minY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, maxY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, minY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, maxY, minZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, minY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(maxX, maxY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, minY, maxZ).color(r, g, b, a).endVertex();
        renderer.pos(minX, maxY, maxZ).color(r, g, b, a).endVertex();

        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    public static void drawCircle3D(Entity entity, float radius, Color color, int points) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosZ;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        for (int i = 0; i <= points; i++) {
            double angle = i * 6.283185307179586 / points;
            renderer.pos(x + radius * Math.cos(angle), y, z + radius * Math.sin(angle))
                    .color(r, g, b, a).endVertex();
        }

        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void drawTracerToEntity(Entity entity, Color color) {
        if (entity == null) return;

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosZ;

        double eyeY = entity.getEyeHeight();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(1.5f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        renderer.pos(0, 0, 0).color(r, g, b, a).endVertex();
        renderer.pos(x, y + eyeY, z).color(r, g, b, a).endVertex();

        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void draw2DESPBox(EntityLivingBase entity, Color color, Color outlineColor) {
        AxisAlignedBB bb = entity.getEntityBoundingBox();

        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks;

        double minX = bb.minX - x;
        double minY = bb.minY - y;
        double minZ = bb.minZ - z;
        double maxX = bb.maxX - x;
        double maxY = bb.maxY - y;
        double maxZ = bb.maxZ - z;

        float[][] corners = new float[8][3];
        int idx = 0;
        for (int corner = 0; corner < 8; corner++) {
            double cx = (corner & 1) == 0 ? minX : maxX;
            double cy = (corner & 2) == 0 ? minY : maxY;
            double cz = (corner & 4) == 0 ? minZ : maxZ;
            corners[idx] = worldToScreen(cx, cy, cz);
            idx++;
        }

        float screenMinX = Float.MAX_VALUE;
        float screenMinY = Float.MAX_VALUE;
        float screenMaxX = -1f;
        float screenMaxY = -1f;

        for (float[] corner : corners) {
            if (corner == null) continue;
            if (corner[2] < 0.1f) return;
            screenMinX = Math.min(corner[0], screenMinX);
            screenMinY = Math.min(corner[1], screenMinY);
            screenMaxX = Math.max(corner[0], screenMaxX);
            screenMaxY = Math.max(corner[1], screenMaxY);
        }

        if (screenMinX >= screenMaxX || screenMinY >= screenMaxY) return;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(2f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(screenMinX, screenMinY, 0).color(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f * 0.2f).endVertex();
        renderer.pos(screenMinX, screenMaxY, 0).color(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f * 0.2f).endVertex();
        renderer.pos(screenMaxX, screenMaxY, 0).color(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f * 0.2f).endVertex();
        renderer.pos(screenMaxX, screenMinY, 0).color(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f * 0.2f).endVertex();
        tessellator.draw();

        renderer.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(screenMinX, screenMinY, 0).color(outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f,
                outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f).endVertex();
        renderer.pos(screenMinX, screenMaxY, 0).color(outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f,
                outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f).endVertex();
        renderer.pos(screenMaxX, screenMaxY, 0).color(outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f,
                outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f).endVertex();
        renderer.pos(screenMaxX, screenMinY, 0).color(outlineColor.getRed() / 255f, outlineColor.getGreen() / 255f,
                outlineColor.getBlue() / 255f, outlineColor.getAlpha() / 255f).endVertex();
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static float[] worldToScreen(double x, double y, double z) {
        float[] pos = new float[3];

        FloatBuffer modelview = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(4);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        boolean result = GLUProject(x, y, z, modelview, projection, viewport, pos);
        if (result) {
            pos[0] /= mc.displayWidth;
            pos[1] /= mc.displayHeight;
            pos[0] *= mc.displayWidth;
            pos[1] *= mc.displayHeight;
        }
        return result ? pos : null;
    }

    private static boolean GLUProject(double objX, double objY, double objZ,
                                       FloatBuffer modelview, FloatBuffer projection, IntBuffer viewport, float[] winPos) {
        float[] in = new float[4];
        float[] out = new float[4];

        in[0] = (float) objX;
        in[1] = (float) objY;
        in[2] = (float) objZ;
        in[3] = 1;

        modelview.rewind();
        for (int i = 0; i < 4; i++) {
            out[i] = 0;
            for (int j = 0; j < 4; j++) {
                out[i] += modelview.get(i + j * 4) * in[j];
            }
        }

        projection.rewind();
        float[] clip = new float[4];
        for (int i = 0; i < 4; i++) {
            clip[i] = 0;
            for (int j = 0; j < 4; j++) {
                clip[i] += projection.get(i + j * 4) * out[j];
            }
        }

        if (clip[3] == 0) return false;

        float w = clip[3];
        for (int i = 0; i < 3; i++) {
            clip[i] /= w;
        }

        int vpX = viewport.get(0);
        int vpY = viewport.get(1);
        int vpW = viewport.get(2);
        int vpH = viewport.get(3);

        winPos[0] = vpX + (1 + clip[0]) * vpW / 2f;
        winPos[1] = vpY + (1 + clip[1]) * vpH / 2f;
        winPos[2] = (1 + clip[2]) / 2f;

        return true;
    }

    public static void drawHealthBar(EntityLivingBase entity, float width, float height) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosY + entity.height + 0.5;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                - mc.getRenderManager().renderPosZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(health / maxHealth, 1f);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(x - width / 2, y, z).color(0, 0, 0, 150).endVertex();
        renderer.pos(x - width / 2, y + height, z).color(0, 0, 0, 150).endVertex();
        renderer.pos(x + width / 2, y + height, z).color(0, 0, 0, 150).endVertex();
        renderer.pos(x + width / 2, y, z).color(0, 0, 0, 150).endVertex();
        tessellator.draw();

        Color healthColor = healthPercent > 0.5f
                ? new Color((int) ((1f - healthPercent) * 2f * 255), 255, 0)
                : new Color(255, (int) (healthPercent * 2f * 255), 0);

        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(x - width / 2, y, z).color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        renderer.pos(x - width / 2, y + height, z).color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        renderer.pos(x - width / 2 + width * healthPercent, y + height, z)
                .color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        renderer.pos(x - width / 2 + width * healthPercent, y, z)
                .color(healthColor.getRed(), healthColor.getGreen(), healthColor.getBlue(), 200).endVertex();
        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
