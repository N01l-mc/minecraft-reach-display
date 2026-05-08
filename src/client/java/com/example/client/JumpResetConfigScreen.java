package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class JumpResetConfigScreen extends Screen {
    private static final TextColor GREEN = TextColor.fromRgb(0x55FF55);
    private static final TextColor RED = TextColor.fromRgb(0xFF5555);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    private final Screen parent;

    public JumpResetConfigScreen(Screen parent) {
        super(Component.translatable("screen.pvp-overlay.jump_reset_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int buttonWidth = 200;
        int sliderWidth = 180;
        int resetWidth = 70;
        int colorBoxWidth = 82;
        int pickerButtonWidth = 24;

        int startY = 54;
        int row = 28;

        Button enabledButton = Button.builder(
                jumpResetButtonText(),
                button -> {
                    TestModClient.toggleShowJumpReset();
                    button.setMessage(jumpResetButtonText());
                }
        ).bounds(centerX - buttonWidth / 2, startY, buttonWidth, 20).build();

        int sliderX = centerX - (sliderWidth + 4 + resetWidth) / 2;

        JumpResetPairWindowSlider pairWindowSlider = new JumpResetPairWindowSlider(
                sliderX,
                startY + row,
                sliderWidth,
                20
        );

        Button resetPairWindowButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetPairWindowTicks();
                    this.rebuildWidgets();
                }
        ).bounds(sliderX + sliderWidth + 4, startY + row, resetWidth, 20).build();

        JumpResetDisplayTicksSlider displayTicksSlider = new JumpResetDisplayTicksSlider(
                sliderX,
                startY + row * 2,
                sliderWidth,
                20
        );

        Button resetDisplayTicksButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetDisplayTicks();
                    this.rebuildWidgets();
                }
        ).bounds(sliderX + sliderWidth + 4, startY + row * 2, resetWidth, 20).build();

        Button unitButton = Button.builder(
                unitButtonText(),
                button -> {
                    TestModClient.cycleJumpResetUnitMode();
                    button.setMessage(unitButtonText());
                }
        ).bounds(centerX - buttonWidth / 2, startY + row * 3, buttonWidth, 20).build();

        int colorBoxX = centerX - 35;
        int pickerButtonX = colorBoxX + colorBoxWidth + 4;
        int colorResetX = pickerButtonX + pickerButtonWidth + 4;

        int perfectY = startY + row * 4 + 6;
        int earlyY = startY + row * 5 + 6;
        int lateY = startY + row * 6 + 6;

        EditBox perfectColorBox = createColorBox(
                colorBoxX,
                perfectY,
                TestModClient.getJumpResetPerfectColorHex(),
                TestModClient::setJumpResetPerfectColorHex
        );

        Button perfectPickerButton = Button.builder(
                Component.literal("🎨"),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.color_picker.perfect"),
                        TestModClient.getJumpResetPerfectColorHex(),
                        TestModClient::setJumpResetPerfectColorHex
                ))
        ).bounds(pickerButtonX, perfectY, pickerButtonWidth, 20).build();

        Button resetPerfectColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetPerfectColor();
                    this.rebuildWidgets();
                }
        ).bounds(colorResetX, perfectY, resetWidth, 20).build();

        EditBox earlyColorBox = createColorBox(
                colorBoxX,
                earlyY,
                TestModClient.getJumpResetEarlyColorHex(),
                TestModClient::setJumpResetEarlyColorHex
        );

        Button earlyPickerButton = Button.builder(
                Component.literal("🎨"),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.color_picker.early"),
                        TestModClient.getJumpResetEarlyColorHex(),
                        TestModClient::setJumpResetEarlyColorHex
                ))
        ).bounds(pickerButtonX, earlyY, pickerButtonWidth, 20).build();

        Button resetEarlyColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetEarlyColor();
                    this.rebuildWidgets();
                }
        ).bounds(colorResetX, earlyY, resetWidth, 20).build();

        EditBox lateColorBox = createColorBox(
                colorBoxX,
                lateY,
                TestModClient.getJumpResetLateColorHex(),
                TestModClient::setJumpResetLateColorHex
        );

        Button latePickerButton = Button.builder(
                Component.literal("🎨"),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.color_picker.late"),
                        TestModClient.getJumpResetLateColorHex(),
                        TestModClient::setJumpResetLateColorHex
                ))
        ).bounds(pickerButtonX, lateY, pickerButtonWidth, 20).build();

        Button resetLateColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.resetJumpResetLateColor();
                    this.rebuildWidgets();
                }
        ).bounds(colorResetX, lateY, resetWidth, 20).build();

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX - buttonWidth / 2, this.height - 28, buttonWidth, 20).build();

        this.addRenderableWidget(enabledButton);
        this.addRenderableWidget(pairWindowSlider);
        this.addRenderableWidget(resetPairWindowButton);
        this.addRenderableWidget(displayTicksSlider);
        this.addRenderableWidget(resetDisplayTicksButton);
        this.addRenderableWidget(unitButton);

        this.addRenderableWidget(perfectColorBox);
        this.addRenderableWidget(perfectPickerButton);
        this.addRenderableWidget(resetPerfectColorButton);

        this.addRenderableWidget(earlyColorBox);
        this.addRenderableWidget(earlyPickerButton);
        this.addRenderableWidget(resetEarlyColorButton);

        this.addRenderableWidget(lateColorBox);
        this.addRenderableWidget(latePickerButton);
        this.addRenderableWidget(resetLateColorButton);

        this.addRenderableWidget(doneButton);
    }

    private EditBox createColorBox(int x, int y, String value, ColorSetter setter) {
        EditBox editBox = new EditBox(
                this.font,
                x,
                y,
                82,
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

        int centerX = this.width / 2;
        int colorLabelX = centerX - 155;

        int startY = 54;
        int row = 28;

        graphics.drawString(
                this.font,
                Component.translatable("label.pvp-overlay.jump_reset_perfect_color"),
                colorLabelX,
                startY + row * 4 + 12,
                0xFFFFFFFF,
                false
        );

        graphics.drawString(
                this.font,
                Component.translatable("label.pvp-overlay.jump_reset_early_color"),
                colorLabelX,
                startY + row * 5 + 12,
                0xFFFFFFFF,
                false
        );

        graphics.drawString(
                this.font,
                Component.translatable("label.pvp-overlay.jump_reset_late_color"),
                colorLabelX,
                startY + row * 6 + 12,
                0xFFFFFFFF,
                false
        );

        super.render(graphics, mouseX, mouseY, delta);
    }

    private Component jumpResetButtonText() {
        MutableComponent component = Component.translatable("button.pvp-overlay.show_jump_reset")
                .append(": ")
                .withStyle(Style.EMPTY.withColor(WHITE));

        MutableComponent state = Component.translatable(TestModClient.isShowJumpReset() ? "state.pvp-overlay.on" : "state.pvp-overlay.off")
                .withStyle(Style.EMPTY.withColor(TestModClient.isShowJumpReset() ? GREEN : RED));

        return component.append(state);
    }

    private Component unitButtonText() {
        return Component.translatable("button.pvp-overlay.jump_reset_unit")
                .append(": ")
                .append(TestModClient.getJumpResetUnitText());
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