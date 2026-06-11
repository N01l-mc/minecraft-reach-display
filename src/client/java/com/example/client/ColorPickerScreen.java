package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.Color;
import java.util.Locale;

public class ColorPickerScreen extends Screen {
    private static final int PICKER_WIDTH = 180;
    private static final int PICKER_HEIGHT = 100;

    private static final int HUE_WIDTH = 12;
    private static final int HUE_HEIGHT = PICKER_HEIGHT;

    private static final int ALPHA_WIDTH = PICKER_WIDTH;
    private static final int ALPHA_HEIGHT = 12;

    private static final int PREVIEW_WIDTH = 44;
    private static final int PREVIEW_HEIGHT = 20;

    private static final int HEX_BOX_WIDTH = 82;
    private static final int HEX_BOX_HEIGHT = 20;

    private static final int BUTTON_WIDTH = 200;

    private static final int PICKER_CELL_SIZE = 6;
    private static final int HUE_CELL_HEIGHT = 4;
    private static final int ALPHA_CELL_WIDTH = 4;
    private static final int CHECKER_SIZE = 4;

    private final Screen parent;
    private final ColorSetter colorSetter;
    private final ColorAlphaSetter colorAlphaSetter;
    private final boolean showAlpha;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float brightness = 1.0f;

    private int alphaPercent = 100;

    private String currentHex = "#FFFFFF";
    private int currentRgb = 0xFFFFFF;

    private boolean draggingPicker = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;
    private boolean colorDirty = false;

    private int pickerX;
    private int pickerY;

    private int hueX;
    private int hueY;

    private int alphaX;
    private int alphaY;

    private int selectedRowY;
    private int previewX;
    private int previewY;

    private EditBox hexBox;

    public ColorPickerScreen(Screen parent, Component title, String initialColor, ColorSetter colorSetter) {
        super(title);
        this.parent = parent;
        this.colorSetter = colorSetter;
        this.colorAlphaSetter = null;
        this.showAlpha = false;

        setColorFromHex(initialColor, false);
    }

    public ColorPickerScreen(
            Screen parent,
            Component title,
            String initialColor,
            int initialAlphaPercent,
            ColorAlphaSetter colorAlphaSetter
    ) {
        super(title);
        this.parent = parent;
        this.colorSetter = null;
        this.colorAlphaSetter = colorAlphaSetter;
        this.showAlpha = true;
        this.alphaPercent = PvPOverlayConfig.clampInt(initialAlphaPercent, 0, 100);

        setColorFromHex(initialColor, false);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        pickerX = centerX - (PICKER_WIDTH + 10 + HUE_WIDTH) / 2;
        pickerY = 46;

        hueX = pickerX + PICKER_WIDTH + 10;
        hueY = pickerY;

        alphaX = pickerX;
        alphaY = pickerY + PICKER_HEIGHT + 10;

        selectedRowY = showAlpha ? alphaY + ALPHA_HEIGHT + 14 : pickerY + PICKER_HEIGHT + 18;

        int selectedLabelWidth = this.font.width(Component.translatable("label.pvp-overlay.color_picker.selected"));
        int selectedRowWidth = selectedLabelWidth + 6 + HEX_BOX_WIDTH + 8 + PREVIEW_WIDTH;

        int selectedX = centerX - selectedRowWidth / 2;
        int hexBoxX = selectedX + selectedLabelWidth + 6;

        previewX = hexBoxX + HEX_BOX_WIDTH + 8;
        previewY = selectedRowY;

        hexBox = new EditBox(
                this.font,
                hexBoxX,
                selectedRowY,
                HEX_BOX_WIDTH,
                HEX_BOX_HEIGHT,
                Component.translatable("screen.pvp-overlay.jump_reset_config.color_input")
        );

        hexBox.setMaxLength(7);
        hexBox.setValue(currentHex);
        hexBox.setResponder(value -> {
            if (PvPOverlayConfig.isValidColorHex(value)) {
                setColorFromHex(value, true);
            }
        });

        this.addRenderableWidget(hexBox);

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> {
                    applyIfDirty();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).bounds(centerX - BUTTON_WIDTH / 2, selectedRowY + 30, BUTTON_WIDTH, 20).build();

        this.addRenderableWidget(doneButton);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (TestModClient.isOpenConfigKey(input)) {
            applyIfDirty();
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        applyIfDirty();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (isInside(mouseX, mouseY, pickerX, pickerY, PICKER_WIDTH, PICKER_HEIGHT)) {
            draggingPicker = true;
            updatePicker(mouseX, mouseY);
            return true;
        }

        if (isInside(mouseX, mouseY, hueX, hueY, HUE_WIDTH, HUE_HEIGHT)) {
            draggingHue = true;
            updateHue(mouseY);
            return true;
        }

        if (showAlpha && isInside(mouseX, mouseY, alphaX, alphaY, ALPHA_WIDTH, ALPHA_HEIGHT)) {
            draggingAlpha = true;
            updateAlpha(mouseX);
            return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (draggingPicker) {
            updatePicker(click.x(), click.y());
            return true;
        }

        if (draggingHue) {
            updateHue(click.y());
            return true;
        }

        if (draggingAlpha) {
            updateAlpha(click.x());
            return true;
        }

        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (draggingPicker || draggingHue || draggingAlpha) {
            draggingPicker = false;
            draggingHue = false;
            draggingAlpha = false;
            applyIfDirty();
            return true;
        }

        applyIfDirty();

        return super.mouseReleased(click);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        TestModClient.drawConfigMenuBackdrop(graphics, this.width, this.height);

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                20,
                0xFFFFFFFF
        );

        drawPicker(graphics);
        drawHueBar(graphics);

        if (showAlpha) {
            drawAlphaBar(graphics);
        }

        drawSelectedRow(graphics);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawPicker(GuiGraphics graphics) {
        for (int x = 0; x < PICKER_WIDTH; x += PICKER_CELL_SIZE) {
            float s = x / (float) (PICKER_WIDTH - 1);
            int cellRight = Math.min(pickerX + x + PICKER_CELL_SIZE, pickerX + PICKER_WIDTH);

            for (int y = 0; y < PICKER_HEIGHT; y += PICKER_CELL_SIZE) {
                float b = 1.0f - y / (float) (PICKER_HEIGHT - 1);
                int rgb = Color.HSBtoRGB(hue, s, b) | 0xFF000000;
                int cellBottom = Math.min(pickerY + y + PICKER_CELL_SIZE, pickerY + PICKER_HEIGHT);

                graphics.fill(
                        pickerX + x,
                        pickerY + y,
                        cellRight,
                        cellBottom,
                        rgb
                );
            }
        }

        graphics.renderOutline(pickerX, pickerY, PICKER_WIDTH, PICKER_HEIGHT, 0xFFFFFFFF);

        int markerX = pickerX + Math.round(saturation * (PICKER_WIDTH - 1));
        int markerY = pickerY + Math.round((1.0f - brightness) * (PICKER_HEIGHT - 1));

        graphics.renderOutline(markerX - 3, markerY - 3, 7, 7, 0xFFFFFFFF);
        graphics.renderOutline(markerX - 2, markerY - 2, 5, 5, 0xFF000000);
    }

    private void drawHueBar(GuiGraphics graphics) {
        for (int y = 0; y < HUE_HEIGHT; y += HUE_CELL_HEIGHT) {
            float h = y / (float) (HUE_HEIGHT - 1);
            int rgb = Color.HSBtoRGB(h, 1.0f, 1.0f) | 0xFF000000;
            int cellBottom = Math.min(hueY + y + HUE_CELL_HEIGHT, hueY + HUE_HEIGHT);

            graphics.fill(
                    hueX,
                    hueY + y,
                    hueX + HUE_WIDTH,
                    cellBottom,
                    rgb
            );
        }

        graphics.renderOutline(hueX, hueY, HUE_WIDTH, HUE_HEIGHT, 0xFFFFFFFF);

        int markerY = hueY + Math.round(hue * (HUE_HEIGHT - 1));
        graphics.renderOutline(hueX - 2, markerY - 2, HUE_WIDTH + 4, 5, 0xFFFFFFFF);
        graphics.renderOutline(hueX - 1, markerY - 1, HUE_WIDTH + 2, 3, 0xFF000000);
    }

    private void drawAlphaBar(GuiGraphics graphics) {
        drawCheckerboard(graphics, alphaX, alphaY, ALPHA_WIDTH, ALPHA_HEIGHT);

        for (int x = 0; x < ALPHA_WIDTH; x += ALPHA_CELL_WIDTH) {
            float alphaFraction = x / (float) (ALPHA_WIDTH - 1);
            int alpha = Math.round(alphaFraction * 255.0f);
            int transparentColor = blendOverCheckerAverage(currentRgb, alpha);

            int cellRight = Math.min(alphaX + x + ALPHA_CELL_WIDTH, alphaX + ALPHA_WIDTH);

            graphics.fill(
                    alphaX + x,
                    alphaY,
                    cellRight,
                    alphaY + ALPHA_HEIGHT,
                    0xFF000000 | transparentColor
            );
        }

        graphics.renderOutline(alphaX, alphaY, ALPHA_WIDTH, ALPHA_HEIGHT, 0xFFFFFFFF);

        int markerX = alphaX + Math.round((alphaPercent / 100.0f) * (ALPHA_WIDTH - 1));
        graphics.renderOutline(markerX - 2, alphaY - 2, 5, ALPHA_HEIGHT + 4, 0xFFFFFFFF);
        graphics.renderOutline(markerX - 1, alphaY - 1, 3, ALPHA_HEIGHT + 2, 0xFF000000);
    }

    private void drawSelectedRow(GuiGraphics graphics) {
        Component selectedLabel = Component.translatable("label.pvp-overlay.color_picker.selected");

        int labelX = hexBox.getX() - 6 - this.font.width(selectedLabel);

        graphics.drawString(
                this.font,
                selectedLabel,
                labelX,
                selectedRowY + 6,
                0xFFAAAAAA,
                false
        );

        drawPreview(graphics);
    }

    private void drawPreview(GuiGraphics graphics) {
        graphics.fill(
                previewX,
                previewY,
                previewX + PREVIEW_WIDTH,
                previewY + PREVIEW_HEIGHT,
                getCurrentArgb()
        );

        graphics.renderOutline(previewX, previewY, PREVIEW_WIDTH, PREVIEW_HEIGHT, 0xFFFFFFFF);
    }

    private void drawCheckerboard(GuiGraphics graphics, int x, int y, int width, int height) {
        for (int cellX = 0; cellX < width; cellX += CHECKER_SIZE) {
            for (int cellY = 0; cellY < height; cellY += CHECKER_SIZE) {
                boolean light = ((cellX / CHECKER_SIZE) + (cellY / CHECKER_SIZE)) % 2 == 0;
                int color = light ? 0xFFE0E0E0 : 0xFF8A8A8A;

                graphics.fill(
                        x + cellX,
                        y + cellY,
                        Math.min(x + cellX + CHECKER_SIZE, x + width),
                        Math.min(y + cellY + CHECKER_SIZE, y + height),
                        color
                );
            }
        }
    }

    private int blendOverCheckerAverage(int rgb, int alpha) {
        int checkerRgb = 0xB5B5B5;

        int r = blendChannel((rgb >> 16) & 0xFF, (checkerRgb >> 16) & 0xFF, alpha);
        int g = blendChannel((rgb >> 8) & 0xFF, (checkerRgb >> 8) & 0xFF, alpha);
        int b = blendChannel(rgb & 0xFF, checkerRgb & 0xFF, alpha);

        return (r << 16) | (g << 8) | b;
    }

    private int blendChannel(int foreground, int background, int alpha) {
        return Math.round((foreground * alpha + background * (255 - alpha)) / 255.0f);
    }

    private int alphaFromPercent(int percent) {
        return Math.round(PvPOverlayConfig.clampInt(percent, 0, 100) * 255.0f / 100.0f);
    }

    private int getCurrentArgb() {
        return (alphaFromPercent(alphaPercent) << 24) | currentRgb;
    }

    private void updatePicker(double mouseX, double mouseY) {
        saturation = clampFloat((float) ((mouseX - pickerX) / (PICKER_WIDTH - 1)), 0.0f, 1.0f);
        brightness = 1.0f - clampFloat((float) ((mouseY - pickerY) / (PICKER_HEIGHT - 1)), 0.0f, 1.0f);

        updateCurrentColor(true);
    }

    private void updateHue(double mouseY) {
        hue = clampFloat((float) ((mouseY - hueY) / (HUE_HEIGHT - 1)), 0.0f, 1.0f);

        updateCurrentColor(true);
    }

    private void updateAlpha(double mouseX) {
        alphaPercent = Math.round(clampFloat((float) ((mouseX - alphaX) / (ALPHA_WIDTH - 1)), 0.0f, 1.0f) * 100.0f);
        colorDirty = true;
    }

    private void updateCurrentColor(boolean markDirty) {
        currentRgb = Color.HSBtoRGB(hue, saturation, brightness) & 0x00FFFFFF;
        currentHex = String.format(Locale.US, "#%06X", currentRgb);

        if (hexBox != null && !hexBox.getValue().equals(currentHex)) {
            hexBox.setValue(currentHex);
        }

        if (markDirty) {
            colorDirty = true;
        }
    }

    private void setColorFromHex(String value, boolean markDirty) {
        String normalized = PvPOverlayConfig.normalizeColorHex(value, "#FFFFFF");
        String raw = normalized.substring(1);

        currentRgb = Integer.parseInt(raw, 16);
        currentHex = normalized;

        float[] hsb = Color.RGBtoHSB(
                (currentRgb >> 16) & 0xFF,
                (currentRgb >> 8) & 0xFF,
                currentRgb & 0xFF,
                null
        );

        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];

        if (hexBox != null && !hexBox.getValue().equals(currentHex)) {
            hexBox.setValue(currentHex);
        }

        if (markDirty) {
            colorDirty = true;
        }
    }

    private void applyIfDirty() {
        if (!colorDirty) {
            return;
        }

        if (showAlpha && colorAlphaSetter != null) {
            colorAlphaSetter.set(currentHex, alphaPercent);
        } else if (colorSetter != null) {
            colorSetter.set(currentHex);
        }

        colorDirty = false;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x &&
                mouseX <= x + width &&
                mouseY >= y &&
                mouseY <= y + height;
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public interface ColorSetter {
        void set(String value);
    }

    public interface ColorAlphaSetter {
        void set(String value, int alphaPercent);
    }
}