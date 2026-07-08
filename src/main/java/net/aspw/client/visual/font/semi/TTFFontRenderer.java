package net.aspw.client.visual.font.semi;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Locale;

public class TTFFontRenderer {

    private static final int MARGIN = 4;

    private static final char COLOR_INVOKER = '\247';

    private static final int RANDOM_OFFSET = 1;

    private final Font font;

    private final CharacterData[] regularData;

    private final CharacterData[] boldData;

    private final CharacterData[] italicsData;

    private final int[] colorCodes = new int[32];

    private final boolean fractionalMetrics;

    public TTFFontRenderer(final Font font) {
        this(font, 256);
    }

    public TTFFontRenderer(final Font font, final int characterCount) {
        this(font, characterCount, true);
    }

    public TTFFontRenderer(final Font font, final int characterCount, final boolean fractionalMetrics) {
        this.font = font;
        this.fractionalMetrics = fractionalMetrics;

        this.regularData = setup(new CharacterData[characterCount], Font.PLAIN);
        this.boldData = setup(new CharacterData[characterCount], Font.BOLD);
        this.italicsData = setup(new CharacterData[characterCount], Font.ITALIC);
    }

    private CharacterData[] setup(final CharacterData[] characterData, final int type) {

        generateColors();

        final Font font = this.font.deriveFont(type);

        final BufferedImage utilityImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D utilityGraphics = (Graphics2D) utilityImage.getGraphics();

        utilityGraphics.setFont(font);

        final FontMetrics fontMetrics = utilityGraphics.getFontMetrics();

        for (int index = 0; index < characterData.length; index++) {

            final char character = (char) index;

            final Rectangle2D characterBounds = fontMetrics.getStringBounds(character + "", utilityGraphics);

            final float width = (float) characterBounds.getWidth() + (2 * MARGIN);

            final float height = (float) characterBounds.getHeight();

            final BufferedImage characterImage = new BufferedImage(MathHelper.ceiling_double_int(width), MathHelper.ceiling_double_int(height), BufferedImage.TYPE_INT_ARGB);

            final Graphics2D graphics = (Graphics2D) characterImage.getGraphics();

            graphics.setFont(font);

            graphics.setColor(new Color(255, 255, 255, 0));

            graphics.fillRect(0, 0, characterImage.getWidth(), characterImage.getHeight());

            graphics.setColor(Color.WHITE);

            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

            graphics.drawString(character + "", MARGIN, fontMetrics.getAscent());

            final int textureId = GlStateManager.generateTexture();

            createTexture(textureId, characterImage);

            characterData[index] = new CharacterData(character, characterImage.getWidth(), characterImage.getHeight(), textureId);
        }

        return characterData;
    }

    private void createTexture(final int textureId, final BufferedImage image) {

        final int[] pixels = new int[image.getWidth() * image.getHeight()];

        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        final ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                final int pixel = pixels[y * image.getWidth() + x];

                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }

        buffer.flip();

        GlStateManager.bindTexture(textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, image.getWidth(), image.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GlStateManager.bindTexture(0);
    }

    public void drawString(final String text, final float x, final float y, final int color) {
        renderString(text, x, y, color, false);
    }

    public void drawStringWithShadow(final String text, final float x, final float y, final int color) {
        GL11.glTranslated(0.5, 0.5, 0);
        renderString(text, x, y, color, true);
        GL11.glTranslated(-0.5, -0.5, 0);
        renderString(text, x, y, color, false);
    }

    private void renderString(final String text, float x, float y, final int color, final boolean shadow) {

        if (text.isEmpty()) return;

        GL11.glPushMatrix();

        GlStateManager.scale(0.5, 0.5, 1);

        x -= (float) MARGIN / 2;
        y -= (float) MARGIN / 2;

        x += 0.5F;
        y += 0.5F;

        x *= 2;
        y *= 2;

        CharacterData[] characterData = regularData;

        boolean underlined = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        final int length = text.length();

        final float multiplier = (shadow ? 4 : 1);

        final float a = (float) (color >> 24 & 255) / 255F;
        final float r = (float) (color >> 16 & 255) / 255F;
        final float g = (float) (color >> 8 & 255) / 255F;
        final float b = (float) (color & 255) / 255F;

        GL11.glColor4f(r / multiplier, g / multiplier, b / multiplier, a);

        for (int i = 0; i < length; i++) {

            char character = text.charAt(i);

            final char previous = i > 0 ? text.charAt(i - 1) : '.';

            if (previous == COLOR_INVOKER) continue;

            if (character == COLOR_INVOKER && i < length) {

                int index = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));

                if (index < 16) {

                    obfuscated = false;
                    strikethrough = false;
                    underlined = false;

                    characterData = regularData;

                    if (index < 0 || index > 15) index = 15;

                    if (shadow) index += 16;

                    final int textColor = this.colorCodes[index];

                    GL11.glColor4d((textColor >> 16) / 255d, (textColor >> 8 & 255) / 255d, (textColor & 255) / 255d, a);
                } else if (index == 16)
                    obfuscated = true;
                else if (index == 17)

                    characterData = boldData;
                else if (index == 18)
                    strikethrough = true;
                else if (index == 19)
                    underlined = true;
                else if (index == 20)

                    characterData = italicsData;
                else if (index == 21) {

                    obfuscated = false;
                    strikethrough = false;
                    underlined = false;

                    characterData = regularData;

                    GL11.glColor4d(1 * (shadow ? 0.25 : 1), 1 * (shadow ? 0.25 : 1), 1 * (shadow ? 0.25 : 1), a);
                }
            } else {

                if (character > 255) continue;

                if (obfuscated)
                    character = (char) (((int) character) + RANDOM_OFFSET);

                drawChar(character, characterData, x, y);

                final CharacterData charData = characterData[character];

                if (strikethrough)
                    drawLine(new Vector2f(0, charData.height / 2f), new Vector2f(charData.width, charData.height / 2f), 3);

                if (underlined)
                    drawLine(new Vector2f(0, charData.height - 15), new Vector2f(charData.width, charData.height - 15), 3);

                x += charData.width - (2 * MARGIN);
            }
        }

        GL11.glPopMatrix();

        GL11.glColor4d(1, 1, 1, 1);

        GlStateManager.bindTexture(0);
    }

    public float getWidth(final String text) {

        float width = 0;

        CharacterData[] characterData = regularData;

        final int length = text.length();

        for (int i = 0; i < length; i++) {

            final char character = text.charAt(i);

            final char previous = i > 0 ? text.charAt(i - 1) : '.';

            if (previous == COLOR_INVOKER) continue;

            if (character == COLOR_INVOKER && i < length) {

                final int index = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));

                if (index == 17)

                    characterData = boldData;
                else if (index == 20)

                    characterData = italicsData;
                else if (index == 21)

                    characterData = regularData;
            } else {

                if (character > 255) continue;

                final CharacterData charData = characterData[character];

                width += (charData.width - (2 * MARGIN)) / 2;
            }
        }

        return width + (float) MARGIN / 2;
    }

    public float getHeight(final String text) {

        float height = 0;

        CharacterData[] characterData = regularData;

        final int length = text.length();

        for (int i = 0; i < length; i++) {

            final char character = text.charAt(i);

            final char previous = i > 0 ? text.charAt(i - 1) : '.';

            if (previous == COLOR_INVOKER) continue;

            if (character == COLOR_INVOKER && i < length) {

                final int index = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));

                if (index == 17)

                    characterData = boldData;
                else if (index == 20)

                    characterData = italicsData;
                else if (index == 21)

                    characterData = regularData;
            } else {

                if (character > 255) continue;

                final CharacterData charData = characterData[character];

                height = Math.max(height, charData.height);
            }
        }

        return height / 2 - (float) MARGIN / 2;
    }

    private void drawChar(final char character, final CharacterData[] characterData, final float x, final float y) {

        final CharacterData charData = characterData[character];

        charData.bind();
        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_BLEND);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glBegin(GL11.GL_QUADS);
        {

            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2d(x, y);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2d(x, y + charData.height);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2d(x + charData.width, y + charData.height);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2d(x + charData.width, y);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private void drawLine(final Vector2f start, final Vector2f end, final float width) {

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL11.glLineWidth(width);

        GL11.glBegin(GL11.GL_LINES);
        {
            GL11.glVertex2f(start.x, start.y);
            GL11.glVertex2f(end.x, end.y);
        }

        GL11.glEnd();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private void generateColors() {

        for (int i = 0; i < 32; i++) {

            final int thingy = (i >> 3 & 1) * 85;

            int red = (i >> 2 & 1) * 170 + thingy;

            int green = (i >> 1 & 1) * 170 + thingy;

            int blue = (i & 1) * 170 + thingy;

            if (i == 6) red += 85;

            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            this.colorCodes[i] = (red & 255) << 16 | (green & 255) << 8 | blue & 255;
        }
    }

    public Font getFont() {
        return font;
    }

    static class CharacterData {

        private final int textureId;

        public char character;

        public float width;

        public float height;

        public CharacterData(final char character, final float width, final float height, final int textureId) {
            this.character = character;
            this.width = width;
            this.height = height;
            this.textureId = textureId;
        }

        public void bind() {

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        }
    }
}
