package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class PvPOverlayConfigScreen extends Screen {
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
        return Component.literal("Overlay: " + onOff(TestModClient.isOverlayEnabled()));
    }

    private Component hitButtonText() {
        return Component.literal("Show Hit: " + onOff(TestModClient.isShowHit()));
    }

    private Component takenButtonText() {
        return Component.literal("Show Taken: " + onOff(TestModClient.isShowTaken()));
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}