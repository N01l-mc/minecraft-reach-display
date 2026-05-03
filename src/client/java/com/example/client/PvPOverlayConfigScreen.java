package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    public PvPOverlayConfigScreen() {
        super(Component.literal("PvP Overlay Config"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 4;

        Button overlayButton = Button.builder(
                overlayButtonText(),
                button -> {
                    TestModClient.toggleOverlayEnabled();
                    button.setMessage(overlayButtonText());
                }
        ).bounds(centerX - 100, startY, 200, 20).build();

        Button hitButton = Button.builder(
                hitButtonText(),
                button -> {
                    TestModClient.toggleShowHit();
                    button.setMessage(hitButtonText());
                }
        ).bounds(centerX - 100, startY + 28, 200, 20).build();

        Button takenButton = Button.builder(
                takenButtonText(),
                button -> {
                    TestModClient.toggleShowTaken();
                    button.setMessage(takenButtonText());
                }
        ).bounds(centerX - 100, startY + 56, 200, 20).build();

        Button doneButton = Button.builder(
                Component.literal("Done"),
                button -> Minecraft.getInstance().setScreen(null)
        ).bounds(centerX - 100, startY + 96, 200, 20).build();

        this.addRenderableWidget(overlayButton);
        this.addRenderableWidget(hitButton);
        this.addRenderableWidget(takenButton);
        this.addRenderableWidget(doneButton);
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
        graphics.fill(0, 0, this.width, this.height, 0x88000000);

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                40,
                0xFFFFFFFF
        );

        super.render(graphics, mouseX, mouseY, delta);
    }

    private Component overlayButtonText() {
        return settingText("Overlay: ", TestModClient.isOverlayEnabled());
    }

    private Component hitButtonText() {
        return settingText("Show Hit: ", TestModClient.isShowHit());
    }

    private Component takenButtonText() {
        return settingText("Show Taken: ", TestModClient.isShowTaken());
    }

    private Component settingText(String label, boolean value) {
        MutableComponent component = Component.literal(label)
                .withStyle(Style.EMPTY.withColor(WHITE));

        MutableComponent state = Component.literal(value ? "ON" : "OFF")
                .withStyle(Style.EMPTY.withColor(value ? GREEN : RED));

        return component.append(state);
    }
}