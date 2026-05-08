package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ColorPickerScreen extends Screen {
    private static final int PICKER_WIDTH = 180;
    private static final int PICKER_HEIGHT = 120;
    private static final int HUE_WIDTH = 14;
    private static final int GAP = 10;

    private static final int PICKER_CELL_SIZE = 6;
    private static final int HUE_CELL_HEIGHT = 4;

    private static final int PICKER_COLUMNS = (PICKER_WIDTH + PICKER_CELL_SIZE - 1) / PICKER_CELL_SIZE;
    private static final int PICKER_ROWS = (PICKER_HEIGHT + PICKER_CELL_SIZE - 1) / PICKER_CELL_SIZE;
    private static final int HUE_ROWS = (PICKER_HEIGHT + HUE_CELL_HEIGHT - 1) / HUE_CELL_HEIGHT;

    private final Screen parent;
    private final ColorSetter setter;

    private final int[][] pickerColors = new int[PICKER_COLUMNS][PICKER_ROWS];
    private final int[] hueColors = new int[HUE_ROWS];

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;

    private int currentRgb = 0xFFFFFF;
    private String currentHex = "#FFFFFF";

    private int cachedHueStep = -1;
    private boolean hueColorsBuilt = false;
    private boolean colorDirty = false;
    private boolean updatingHexInput = false;

    private boolean draggingPicker = false;
    private boolean draggingHue = false;

    private EditBox hexInput;

    public ColorPickerScreen(Screen parent, Component title, String initialHex, ColorSetter setter) {
        super(title);
        this.parent = parent;
        this.setter = setter;

        setFromHex(initialHex);
        updateCurrentColorCache();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int pickerX = getPickerX();
        int pickerY = getPickerY();

        hexInput = new EditBox(
                this.font,
                pickerX + 130,
                pickerY + PICKER_HEIGHT + 12,
                82,
                20,
                Component.translatable("screen.pvp-overlay.jump_reset_config.color_input")
        );

        hexInput.setMaxLength(7);
        hexInput.setValue(currentHex);
        hexInput.setResponder(input -> {
            if (updatingHexInput) {
                return;
            }

            if (PvPOverlayConfig.isValidColorHex(input)) {
                setFromHex(input);
                updateCurrentColorCache();
                colorDirty = true;
            }
        });

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> {
                    commitColorIfDirty();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).bounds(centerX - 100, this.height - 28, 200, 20).build();

        this.addRenderableWidget(hexInput);
        this.addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        commitColorIfDirty();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();

        int pickerX = getPickerX();
        int pickerY = getPickerY();
        int hueX = getHueX();
        int hueY = getPickerY();

        if (isInside(mouseX, mouseY, pickerX, pickerY, PICKER_WIDTH, PICKER_HEIGHT)) {
            draggingPicker = true;
            updateSaturationValue(mouseX, mouseY);
            return true;
        }

        if (isInside(mouseX, mouseY, hueX, hueY, HUE_WIDTH, PICKER_HEIGHT)) {
            draggingHue = true;
            updateHue(mouseY);
            return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (draggingPicker) {
            updateSaturationValue(mouseX, mouseY);
            return true;
        }

        if (draggingHue) {
            updateHue(mouseY);
            return true;
        }

        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        boolean wasDragging = draggingPicker || draggingHue;

        draggingPicker = false;
        draggingHue = false;

        if (wasDragging) {
            commitColorIfDirty();
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        TestModClient.drawConfigMenuBackdrop(graphics, this.width, this.height);

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                24,
                0xFFFFFFFF
        );

        int pickerX = getPickerX();
        int pickerY = getPickerY();
        int hueX = getHueX();
        int hueY = pickerY;

        ensureColorCaches();

        drawSaturationValuePicker(graphics, pickerX, pickerY);
        drawHueSlider(graphics, hueX, hueY);
        drawSelectionMarkers(graphics, pickerX, pickerY, hueX, hueY);
        drawPreview(graphics, pickerX, pickerY + PICKER_HEIGHT + 12);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void ensureColorCaches() {
        buildHueColorsIfNeeded();

        int hueStep = Math.round(hue * 360.0f);

        if (hueStep != cachedHueStep) {
            cachedHueStep = hueStep;
            buildPickerColors();
        }
    }

    private void buildPickerColors() {
        for (int column = 0; column < PICKER_COLUMNS; column++) {
            int px = column * PICKER_CELL_SIZE;
            float s = px / (float) (PICKER_WIDTH - 1);

            for (int row = 0; row < PICKER_ROWS; row++) {
                int py = row * PICKER_CELL_SIZE;
                float v = 1.0f - py / (float) (PICKER_HEIGHT - 1);

                pickerColors[column][row] = 0xFF000000 | hsvToRgb(hue, s, v);
            }
        }
    }

    private void buildHueColorsIfNeeded() {
        if (hueColorsBuilt) {
            return;
        }

        hueColorsBuilt = true;

        for (int row = 0; row < HUE_ROWS; row++) {
            int py = row * HUE_CELL_HEIGHT;
            float h = py / (float) (PICKER_HEIGHT - 1);

            hueColors[row] = 0xFF000000 | hsvToRgb(h, 1.0f, 1.0f);
        }
    }

    private void drawSaturationValuePicker(GuiGraphics graphics, int x, int y) {
        for (int column = 0; column < PICKER_COLUMNS; column++) {
            int px = column * PICKER_CELL_SIZE;

            for (int row = 0; row < PICKER_ROWS; row++) {
                int py = row * PICKER_CELL_SIZE;

                int cellRight = Math.min(x + px + PICKER_CELL_SIZE, x + PICKER_WIDTH);
                int cellBottom = Math.min(y + py + PICKER_CELL_SIZE, y + PICKER_HEIGHT);

                graphics.fill(x + px, y + py, cellRight, cellBottom, pickerColors[column][row]);
            }
        }

        graphics.renderOutline(x, y, PICKER_WIDTH, PICKER_HEIGHT, 0xFFFFFFFF);
    }

    private void drawHueSlider(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < HUE_ROWS; row++) {
            int py = row * HUE_CELL_HEIGHT;
            int cellBottom = Math.min(y + py + HUE_CELL_HEIGHT, y + PICKER_HEIGHT);

            graphics.fill(x, y + py, x + HUE_WIDTH, cellBottom, hueColors[row]);
        }

        graphics.renderOutline(x, y, HUE_WIDTH, PICKER_HEIGHT, 0xFFFFFFFF);
    }

    private void drawSelectionMarkers(GuiGraphics graphics, int pickerX, int pickerY, int hueX, int hueY) {
        int selectedX = pickerX + Math.round(saturation * (PICKER_WIDTH - 1));
        int selectedY = pickerY + Math.round((1.0f - value) * (PICKER_HEIGHT - 1));

        graphics.renderOutline(selectedX - 3, selectedY - 3, 7, 7, 0xFF000000);
        graphics.renderOutline(selectedX - 2, selectedY - 2, 5, 5, 0xFFFFFFFF);

        int hueMarkerY = hueY + Math.round(hue * (PICKER_HEIGHT - 1));

        graphics.fill(hueX - 2, hueMarkerY - 1, hueX + HUE_WIDTH + 2, hueMarkerY + 2, 0xFFFFFFFF);
        graphics.fill(hueX - 1, hueMarkerY, hueX + HUE_WIDTH + 1, hueMarkerY + 1, 0xFF000000);
    }

    private void drawPreview(GuiGraphics graphics, int x, int y) {
        graphics.drawString(
                this.font,
                Component.translatable("label.pvp-overlay.color_picker.selected"),
                x,
                y + 5,
                0xFFFFFFFF,
                false
        );

        graphics.fill(x + 80, y, x + 120, y + 20, 0xFF000000 | currentRgb);
        graphics.renderOutline(x + 80, y, 40, 20, 0xFFFFFFFF);
    }

    private void updateSaturationValue(double mouseX, double mouseY) {
        int pickerX = getPickerX();
        int pickerY = getPickerY();

        float newSaturation = clamp01((float) ((mouseX - pickerX) / (PICKER_WIDTH - 1)));
        float newValue = clamp01(1.0f - (float) ((mouseY - pickerY) / (PICKER_HEIGHT - 1)));

        if (newSaturation != saturation || newValue != value) {
            saturation = newSaturation;
            value = newValue;
            updateCurrentColorCache();
            updateHexInputFromCurrentColor();
            colorDirty = true;
        }
    }

    private void updateHue(double mouseY) {
        int hueY = getPickerY();

        float newHue = clamp01((float) ((mouseY - hueY) / (PICKER_HEIGHT - 1)));

        if (newHue != hue) {
            hue = newHue;
            updateCurrentColorCache();
            updateHexInputFromCurrentColor();
            colorDirty = true;
        }
    }

    private void updateCurrentColorCache() {
        currentRgb = hsvToRgb(hue, saturation, value);
        currentHex = String.format("#%06X", currentRgb);
    }

    private void updateHexInputFromCurrentColor() {
        if (hexInput == null) {
            return;
        }

        updatingHexInput = true;
        hexInput.setValue(currentHex);
        updatingHexInput = false;
    }

    private void commitColorIfDirty() {
        if (!colorDirty) {
            return;
        }

        colorDirty = false;
        setter.set(currentHex);
    }

    private void setFromHex(String hex) {
        if (!PvPOverlayConfig.isValidColorHex(hex)) {
            return;
        }

        String normalized = hex.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        int rgb = Integer.parseInt(normalized, 16);

        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        value = max;
        saturation = max == 0.0f ? 0.0f : delta / max;

        if (delta == 0.0f) {
            hue = 0.0f;
        } else if (max == r) {
            hue = ((g - b) / delta) / 6.0f;

            if (hue < 0.0f) {
                hue += 1.0f;
            }
        } else if (max == g) {
            hue = (((b - r) / delta) + 2.0f) / 6.0f;
        } else {
            hue = (((r - g) / delta) + 4.0f) / 6.0f;
        }
    }

    private static int hsvToRgb(float h, float s, float v) {
        h = clamp01(h);
        s = clamp01(s);
        v = clamp01(v);

        float scaledHue = h * 6.0f;
        int sector = (int) Math.floor(scaledHue);
        float fraction = scaledHue - sector;

        float p = v * (1.0f - s);
        float q = v * (1.0f - fraction * s);
        float t = v * (1.0f - (1.0f - fraction) * s);

        float r;
        float g;
        float b;

        switch (sector % 6) {
            case 0 -> {
                r = v;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = v;
                b = p;
            }
            case 2 -> {
                r = p;
                g = v;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = v;
            }
            case 4 -> {
                r = t;
                g = p;
                b = v;
            }
            default -> {
                r = v;
                g = p;
                b = q;
            }
        }

        int red = Math.round(r * 255.0f);
        int green = Math.round(g * 255.0f);
        int blue = Math.round(b * 255.0f);

        return (red << 16) | (green << 8) | blue;
    }

    private int getPickerX() {
        return this.width / 2 - (PICKER_WIDTH + GAP + HUE_WIDTH) / 2;
    }

    private int getPickerY() {
        return 58;
    }

    private int getHueX() {
        return getPickerX() + PICKER_WIDTH + GAP;
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x &&
                mouseX <= x + width &&
                mouseY >= y &&
                mouseY <= y + height;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public interface ColorSetter {
        void set(String value);
    }
}