package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

public class JumpResetConfigScreen extends Screen {
    private static final TextColor GREEN = TextColor.fromRgb(0x55FF55);
    private static final TextColor RED = TextColor.fromRgb(0xFF5555);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    private static final int BUTTON_WIDTH = 200;
    private static final int SLIDER_WIDTH = 144;
    private static final int RESET_WIDTH = 52;
    private static final int COLOR_BOX_WIDTH = 82;
    private static final int PICKER_BUTTON_WIDTH = 24;

    private static final int TOP_FIXED_HEIGHT = 50;
    private static final int BOTTOM_FIXED_HEIGHT = 36;

    private static final int CONTENT_START_Y = 58;
    private static final int ROW_HEIGHT = 28;

    private static final int SCROLL_SPEED = 18;

    private static final Identifier COLOR_PALETTE_TEXTURE = Identifier.fromNamespaceAndPath(
            "pvp-overlay",
            "textures/gui/config-button-color-palette.png"
    );

    private static final int COLOR_PALETTE_ICON_DRAW_SIZE = 14;

    private final Screen parent;

    private int scrollY = 0;

    private Button perfectPickerButton;
    private Button earlyPickerButton;
    private Button latePickerButton;

    public JumpResetConfigScreen(Screen parent) {
        super(Component.translatable("screen.pvp-overlay.jump_reset_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clampScroll();

        int centerX = this.width / 2;

        int leftX = centerX - 205;
        int rightX = centerX + 5;

        int y0 = scrolledY(CONTENT_START_Y);

        Button enabledButton = Button.builder(
                jumpResetButtonText(),
                button -> {
                    TestModClient.toggleShowJumpReset();
                    button.setMessage(jumpResetButtonText());
                }
        ).bounds(leftX, y0, BUTTON_WIDTH, 20).build();

        Button requireSprintButton = Button.builder(
                requireSprintButtonText(),
                button -> {
                    TestModClient.toggleJumpResetRequireSprint();
                    button.setMessage(requireSprintButtonText());
                }
        ).bounds(leftX, y0 + ROW_HEIGHT, BUTTON_WIDTH, 20).build();

        Button timingLabelsButton = Button.builder(
                timingLabelsButtonText(),
                button -> {
                    TestModClient.toggleJumpResetShowTimingLabels();
                    button.setMessage(timingLabelsButtonText());
                }
        ).bounds(leftX, y0 + ROW_HEIGHT * 2, BUTTON_WIDTH, 20).build();

        Button unitButton = Button.builder(
                unitButtonText(),
                button -> {
                    TestModClient.cycleJumpResetUnitMode();
                    button.setMessage(unitButtonText());
                }
        ).bounds(leftX, y0 + ROW_HEIGHT * 3, BUTTON_WIDTH, 20).build();

        JumpResetPairWindowSlider pairWindowSlider = new JumpResetPairWindowSlider(
                rightX,
                y0,
                SLIDER_WIDTH,
                20
        );

        Button resetPairWindowButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetPairWindowTicks();
                    this.rebuildWidgets();
                }
        ).bounds(rightX + SLIDER_WIDTH + 4, y0, RESET_WIDTH, 20).build();

        JumpResetDisplayTicksSlider displayTicksSlider = new JumpResetDisplayTicksSlider(
                rightX,
                y0 + ROW_HEIGHT,
                SLIDER_WIDTH,
                20
        );

        Button resetDisplayTicksButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetDisplayTicks();
                    this.rebuildWidgets();
                }
        ).bounds(rightX + SLIDER_WIDTH + 4, y0 + ROW_HEIGHT, RESET_WIDTH, 20).build();

        int colorBoxX = centerX - 35;
        int pickerButtonX = colorBoxX + COLOR_BOX_WIDTH + 4;
        int colorResetX = pickerButtonX + PICKER_BUTTON_WIDTH + 4;

        int perfectY = y0 + ROW_HEIGHT * 5;
        int earlyY = y0 + ROW_HEIGHT * 6;
        int lateY = y0 + ROW_HEIGHT * 7;

        EditBox perfectColorBox = createColorBox(
                colorBoxX,
                perfectY,
                TestModClient.getJumpResetPerfectColorHex(),
                TestModClient::setJumpResetPerfectColorHex
        );

        perfectPickerButton = Button.builder(
                Component.literal(" "),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.color_picker.perfect"),
                        TestModClient.getJumpResetPerfectColorHex(),
                        TestModClient::setJumpResetPerfectColorHex
                ))
        ).bounds(pickerButtonX, perfectY, PICKER_BUTTON_WIDTH, 20).build();

        Button resetPerfectColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetPerfectColor();
                    this.rebuildWidgets();
                }
        ).bounds(colorResetX, perfectY, RESET_WIDTH, 20).build();

        EditBox earlyColorBox = createColorBox(
                colorBoxX,
                earlyY,
                TestModClient.getJumpResetEarlyColorHex(),
                TestModClient::setJumpResetEarlyColorHex
        );

        earlyPickerButton = Button.builder(
                Component.literal(" "),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.color_picker.early"),
                        TestModClient.getJumpResetEarlyColorHex(),
                        TestModClient::setJumpResetEarlyColorHex
                ))
        ).bounds(pickerButtonX, earlyY, PICKER_BUTTON_WIDTH, 20).build();

        Button resetEarlyColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetEarlyColor();
                    this.rebuildWidgets();
                }
        ).bounds(colorResetX, earlyY, RESET_WIDTH, 20).build();

        EditBox lateColorBox = createColorBox(
                colorBoxX,
                lateY,
                TestModClient.getJumpResetLateColorHex(),
                TestModClient::setJumpResetLateColorHex
        );

        latePickerButton = Button.builder(
                Component.literal(" "),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.color_picker.late"),
                        TestModClient.getJumpResetLateColorHex(),
                        TestModClient::setJumpResetLateColorHex
                ))
        ).bounds(pickerButtonX, lateY, PICKER_BUTTON_WIDTH, 20).build();

        Button resetLateColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetLateColor();
                    this.rebuildWidgets();
                }
        ).bounds(colorResetX, lateY, RESET_WIDTH, 20).build();

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX - BUTTON_WIDTH / 2, this.height - 28, BUTTON_WIDTH, 20).build();

        addScrollableWidget(enabledButton);
        addScrollableWidget(requireSprintButton);
        addScrollableWidget(timingLabelsButton);
        addScrollableWidget(unitButton);

        addScrollableWidget(pairWindowSlider);
        addScrollableWidget(resetPairWindowButton);
        addScrollableWidget(displayTicksSlider);
        addScrollableWidget(resetDisplayTicksButton);

        addScrollableWidget(perfectColorBox);
        addScrollableWidget(perfectPickerButton);
        addScrollableWidget(resetPerfectColorButton);

        addScrollableWidget(earlyColorBox);
        addScrollableWidget(earlyPickerButton);
        addScrollableWidget(resetEarlyColorButton);

        addScrollableWidget(lateColorBox);
        addScrollableWidget(latePickerButton);
        addScrollableWidget(resetLateColorButton);

        this.addRenderableWidget(doneButton);
    }

    private void addScrollableWidget(AbstractWidget widget) {
        widget.visible = isWidgetInScrollableViewport(widget);
        this.addRenderableWidget(widget);
    }

    private boolean isWidgetInScrollableViewport(AbstractWidget widget) {
        int widgetTop = widget.getY();
        int widgetBottom = widget.getY() + widget.getHeight();

        return widgetTop >= TOP_FIXED_HEIGHT &&
                widgetBottom <= this.height - BOTTOM_FIXED_HEIGHT;
    }

    private EditBox createColorBox(int x, int y, String value, ColorSetter setter) {
        EditBox editBox = new EditBox(
                this.font,
                x,
                y,
                COLOR_BOX_WIDTH,
                20,
                Component.translatable("screen.pvp-overlay.jump_reset_config.color_input")
        );

        editBox.setMaxLength(7);
        editBox.setValue(value);
        editBox.setResponder(input -> {
            if (PvPOverlayConfig.isValidColorHex(input)) {
                setter.set(input);
            }
        });

        return editBox;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int oldScrollY = scrollY;

        scrollY -= (int) Math.round(verticalAmount * SCROLL_SPEED);
        clampScroll();

        if (scrollY != oldScrollY) {
            this.rebuildWidgets();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (TestModClient.isOpenConfigKey(input)) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        TestModClient.drawConfigMenuBackdrop(graphics, this.width, this.height);

        drawColorLabels(graphics);

        super.render(graphics, mouseX, mouseY, delta);

        drawPaletteIconIfVisible(graphics, perfectPickerButton);
        drawPaletteIconIfVisible(graphics, earlyPickerButton);
        drawPaletteIconIfVisible(graphics, latePickerButton);

        graphics.fill(0, 0, this.width, TOP_FIXED_HEIGHT, TestModClient.getConfigMenuBackgroundColor());
        graphics.fill(0, this.height - BOTTOM_FIXED_HEIGHT, this.width, this.height, TestModClient.getConfigMenuBackgroundColor());

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                20,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.pvp-overlay.jump_reset_config.description"),
                this.width / 2,
                36,
                0xFFAAAAAA
        );

        drawScrollBar(graphics);
    }

    private void drawPaletteIconIfVisible(GuiGraphics graphics, Button button) {
        if (button == null || !button.visible) {
            return;
        }

        int startX = button.getX() + (button.getWidth() - COLOR_PALETTE_ICON_DRAW_SIZE) / 2;
        int startY = button.getY() + (button.getHeight() - COLOR_PALETTE_ICON_DRAW_SIZE) / 2;

        int iconColor = getButtonIconColor(button);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                COLOR_PALETTE_TEXTURE,
                startX,
                startY,
                0.0F,
                0.0F,
                COLOR_PALETTE_ICON_DRAW_SIZE,
                COLOR_PALETTE_ICON_DRAW_SIZE,
                COLOR_PALETTE_ICON_DRAW_SIZE,
                COLOR_PALETTE_ICON_DRAW_SIZE,
                iconColor
        );
    }

    private int getButtonIconColor(Button button) {
        if (!button.active) {
            return 0xFFA0A0A0;
        }

        if (button.isHoveredOrFocused()) {
            return 0xFFFFFFFF;
        }

        return 0xFFE0E0E0;
    }

    private void drawColorLabels(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int colorLabelX = centerX - 155;

        int y0 = scrolledY(CONTENT_START_Y);

        int perfectY = y0 + ROW_HEIGHT * 5;
        int earlyY = y0 + ROW_HEIGHT * 6;
        int lateY = y0 + ROW_HEIGHT * 7;

        drawColoredColorLabel(
                graphics,
                Component.translatable("overlay.pvp-overlay.jump_reset.perfect").getString(),
                TestModClient.getJumpResetPerfectColorHex(),
                0xFF55FF55,
                colorLabelX,
                perfectY + 6
        );

        drawColoredColorLabel(
                graphics,
                Component.translatable("overlay.pvp-overlay.jump_reset.early").getString(),
                TestModClient.getJumpResetEarlyColorHex(),
                0xFFFFFF55,
                colorLabelX,
                earlyY + 6
        );

        drawColoredColorLabel(
                graphics,
                Component.translatable("overlay.pvp-overlay.jump_reset.late").getString(),
                TestModClient.getJumpResetLateColorHex(),
                0xFFFF5555,
                colorLabelX,
                lateY + 6
        );
    }

    private void drawColoredColorLabel(
            GuiGraphics graphics,
            String timingText,
            String colorHex,
            int fallbackColor,
            int x,
            int y
    ) {
        if (y < TOP_FIXED_HEIGHT || y > this.height - BOTTOM_FIXED_HEIGHT - this.font.lineHeight) {
            return;
        }

        int timingColor = parseColorHex(colorHex, fallbackColor);
        String suffix = Component.translatable("label.pvp-overlay.color_suffix").getString();

        graphics.drawString(
                this.font,
                timingText,
                x,
                y,
                timingColor,
                false
        );

        graphics.drawString(
                this.font,
                suffix,
                x + this.font.width(timingText),
                y,
                0xFFFFFFFF,
                false
        );
    }

    private int parseColorHex(String value, int fallback) {
        if (!PvPOverlayConfig.isValidColorHex(value)) {
            return fallback;
        }

        String normalized = value.trim();

        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        return 0xFF000000 | Integer.parseInt(normalized, 16);
    }

    private void drawScrollBar(GuiGraphics graphics) {
        int maxScroll = getMaxScroll();

        if (maxScroll <= 0) {
            return;
        }

        int trackX = this.width - 8;
        int trackY = TOP_FIXED_HEIGHT + 4;
        int trackHeight = this.height - TOP_FIXED_HEIGHT - BOTTOM_FIXED_HEIGHT - 8;

        if (trackHeight <= 12) {
            return;
        }

        int contentHeight = getScrollableContentHeight();
        int viewportHeight = getScrollableViewportHeight();

        int thumbHeight = Math.max(16, trackHeight * viewportHeight / Math.max(viewportHeight, contentHeight));
        int thumbTravel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = trackY + scrollY * thumbTravel / maxScroll;

        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0x66000000);
        graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xAAFFFFFF);
    }

    private int scrolledY(int y) {
        return y - scrollY;
    }

    private int getScrollableContentHeight() {
        int bottomY = CONTENT_START_Y + ROW_HEIGHT * 7 + 20;
        return bottomY - CONTENT_START_Y;
    }

    private int getScrollableViewportHeight() {
        return Math.max(1, this.height - TOP_FIXED_HEIGHT - BOTTOM_FIXED_HEIGHT);
    }

    private int getMaxScroll() {
        int contentBottom = CONTENT_START_Y + ROW_HEIGHT * 7 + 20;
        int viewportBottom = this.height - BOTTOM_FIXED_HEIGHT;

        return Math.max(0, contentBottom - viewportBottom);
    }

    private void clampScroll() {
        scrollY = clampInt(scrollY, 0, getMaxScroll());
    }

    private Component jumpResetButtonText() {
        return settingText("button.pvp-overlay.show_jump_reset", TestModClient.isShowJumpReset());
    }

    private Component requireSprintButtonText() {
        return settingText("button.pvp-overlay.jump_reset_require_sprint", TestModClient.isJumpResetRequireSprint());
    }

    private Component timingLabelsButtonText() {
        return settingText("button.pvp-overlay.jump_reset_show_timing_labels", TestModClient.isJumpResetShowTimingLabels());
    }

    private Component unitButtonText() {
        return Component.translatable("button.pvp-overlay.jump_reset_unit")
                .append(": ")
                .append(TestModClient.getJumpResetUnitText());
    }

    private Component settingText(String labelKey, boolean value) {
        MutableComponent component = Component.translatable(labelKey)
                .append(": ")
                .withStyle(Style.EMPTY.withColor(WHITE));

        MutableComponent state = Component.translatable(value ? "state.pvp-overlay.on" : "state.pvp-overlay.off")
                .withStyle(Style.EMPTY.withColor(value ? GREEN : RED));

        return component.append(state);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private interface ColorSetter {
        void set(String value);
    }

    private static class JumpResetPairWindowSlider extends AbstractSliderButton {
        public JumpResetPairWindowSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    pairWindowText(TestModClient.getJumpResetPairWindowTicks()),
                    valueFromTicks(TestModClient.getJumpResetPairWindowTicks())
            );
        }

        @Override
        protected void updateMessage() {
            this.setMessage(pairWindowText(ticksFromValue(this.value)));
        }

        @Override
        protected void applyValue() {
            TestModClient.setJumpResetPairWindowTicks(ticksFromValue(this.value));
        }

        private static double valueFromTicks(int ticks) {
            int min = PvPOverlayConfig.MIN_JUMP_RESET_PAIR_WINDOW_TICKS;
            int max = PvPOverlayConfig.MAX_JUMP_RESET_PAIR_WINDOW_TICKS;

            if (max == min) {
                return 0.0;
            }

            return (ticks - min) / (double) (max - min);
        }

        private static int ticksFromValue(double value) {
            int min = PvPOverlayConfig.MIN_JUMP_RESET_PAIR_WINDOW_TICKS;
            int max = PvPOverlayConfig.MAX_JUMP_RESET_PAIR_WINDOW_TICKS;

            return min + (int) Math.round(value * (max - min));
        }

        private static Component pairWindowText(int ticks) {
            return Component.translatable("slider.pvp-overlay.jump_reset_pair_window", ticks);
        }
    }

    private static class JumpResetDisplayTicksSlider extends AbstractSliderButton {
        public JumpResetDisplayTicksSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    displayTicksText(TestModClient.getJumpResetDisplayTicks()),
                    valueFromTicks(TestModClient.getJumpResetDisplayTicks())
            );
        }

        @Override
        protected void updateMessage() {
            this.setMessage(displayTicksText(ticksFromValue(this.value)));
        }

        @Override
        protected void applyValue() {
            TestModClient.setJumpResetDisplayTicks(ticksFromValue(this.value));
        }

        private static double valueFromTicks(int ticks) {
            int min = PvPOverlayConfig.MIN_JUMP_RESET_DISPLAY_TICKS;
            int max = PvPOverlayConfig.MAX_JUMP_RESET_DISPLAY_TICKS;

            if (max == min) {
                return 0.0;
            }

            return (ticks - min) / (double) (max - min);
        }

        private static int ticksFromValue(double value) {
            int min = PvPOverlayConfig.MIN_JUMP_RESET_DISPLAY_TICKS;
            int max = PvPOverlayConfig.MAX_JUMP_RESET_DISPLAY_TICKS;

            return min + (int) Math.round(value * (max - min));
        }

        private static Component displayTicksText(int ticks) {
            return Component.translatable("slider.pvp-overlay.jump_reset_display_ticks", ticks);
        }
    }
}