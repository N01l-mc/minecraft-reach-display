package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class PvPOverlayConfigScreen extends Screen {
    private static final TextColor GREEN = TextColor.fromRgb(0x55FF55);
    private static final TextColor RED = TextColor.fromRgb(0xFF5555);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    private static final int DEFAULT_MENU_OPACITY = 25;

    public PvPOverlayConfigScreen() {
        super(Component.translatable("screen.pvp-overlay.config.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        int buttonWidth = 200;
        int sliderWidth = 144;
        int resetWidth = 52;

        int leftX = centerX - 205;
        int rightX = centerX + 5;

        int startY = 58;
        int row = 28;

        Button overlayButton = Button.builder(
                overlayButtonText(),
                button -> {
                    TestModClient.toggleOverlayEnabled();
                    button.setMessage(overlayButtonText());
                }
        ).bounds(leftX, startY, buttonWidth, 20).build();

        Button hitButton = Button.builder(
                hitButtonText(),
                button -> {
                    TestModClient.toggleShowHit();
                    button.setMessage(hitButtonText());
                }
        ).bounds(leftX, startY + row, buttonWidth, 20).build();

        Button takenButton = Button.builder(
                takenButtonText(),
                button -> {
                    TestModClient.toggleShowTaken();
                    button.setMessage(takenButtonText());
                }
        ).bounds(leftX, startY + row * 2, buttonWidth, 20).build();

        Button forceMainHandButton = Button.builder(
                forceMainHandButtonText(),
                button -> {
                    TestModClient.cycleForcedOtherPlayerMainHand();
                    button.setMessage(forceMainHandButtonText());
                }
        ).bounds(rightX, startY, buttonWidth, 20).build();

        Button changePositionButton = Button.builder(
                Component.translatable("button.pvp-overlay.change_position"),
                button -> Minecraft.getInstance().setScreen(new PvPOverlayPositionScreen(this))
        ).bounds(rightX, startY + row, buttonWidth, 20).build();

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(rightX, startY + row * 2, buttonWidth, 20).build();

        int sliderX = centerX - 100;
        int sliderY = startY + row * 3 + 10;

        ConfigOpacitySlider opacitySlider = new ConfigOpacitySlider(
                sliderX,
                sliderY,
                sliderWidth,
                20
        );

        Button resetOpacityButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.setConfigMenuOpacityPercent(DEFAULT_MENU_OPACITY);
                    this.rebuildWidgets();
                }
        ).bounds(sliderX + sliderWidth + 4, sliderY, resetWidth, 20).build();

        this.addRenderableWidget(overlayButton);
        this.addRenderableWidget(hitButton);
        this.addRenderableWidget(takenButton);
        this.addRenderableWidget(forceMainHandButton);
        this.addRenderableWidget(changePositionButton);
        this.addRenderableWidget(doneButton);
        this.addRenderableWidget(opacitySlider);
        this.addRenderableWidget(resetOpacityButton);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (TestModClient.isOpenConfigKey(input)) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        TestModClient.drawConfigMenuBackdrop(graphics, this.width, this.height);

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                26,
                0xFFFFFFFF
        );

        super.render(graphics, mouseX, mouseY, delta);
    }

    private Component overlayButtonText() {
        return settingText("button.pvp-overlay.overlay", TestModClient.isOverlayEnabled());
    }

    private Component hitButtonText() {
        return settingText("button.pvp-overlay.show_hit", TestModClient.isShowHit());
    }

    private Component takenButtonText() {
        return settingText("button.pvp-overlay.show_taken", TestModClient.isShowTaken());
    }

    private Component forceMainHandButtonText() {
        MutableComponent component = Component.translatable("button.pvp-overlay.force_main_hand")
                .append(": ")
                .withStyle(Style.EMPTY.withColor(WHITE));

        MutableComponent state = TestModClient.getForcedOtherPlayerMainHandText().copy()
                .withStyle(Style.EMPTY.withColor(TestModClient.getForcedOtherPlayerMainHand() == 0 ? RED : GREEN));

        return component.append(state);
    }

    private Component settingText(String labelKey, boolean value) {
        MutableComponent component = Component.translatable(labelKey)
                .append(": ")
                .withStyle(Style.EMPTY.withColor(WHITE));

        MutableComponent state = Component.translatable(value ? "state.pvp-overlay.on" : "state.pvp-overlay.off")
                .withStyle(Style.EMPTY.withColor(value ? GREEN : RED));

        return component.append(state);
    }

    private static class ConfigOpacitySlider extends AbstractSliderButton {
        public ConfigOpacitySlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    opacityText(TestModClient.getConfigMenuOpacityPercent()),
                    TestModClient.getConfigMenuOpacityPercent() / 100.0
            );
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0);
            this.setMessage(opacityText(percent));
        }

        @Override
        protected void applyValue() {
            int percent = (int) Math.round(this.value * 100.0);
            TestModClient.setConfigMenuOpacityPercent(percent);
        }

        private static Component opacityText(int percent) {
            return Component.translatable("slider.pvp-overlay.opacity", percent);
        }
    }
}