package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class ConfirmActionScreen extends Screen {
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private final Component message;
    private final Runnable onConfirm;

    public ConfirmActionScreen(Screen parent, Component title, Component message, Runnable onConfirm) {
        super(title);
        this.parent = parent;
        this.message = message;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 28;

        Button okButton = Button.builder(
                Component.translatable("button.pvp-overlay.ok"),
                button -> {
                    onConfirm.run();
                    Minecraft.getInstance().setScreen(parent);
                }
        ).bounds(centerX - BUTTON_WIDTH - 4, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        Button cancelButton = Button.builder(
                Component.translatable("button.pvp-overlay.cancel"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX + 4, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        this.addRenderableWidget(okButton);
        this.addRenderableWidget(cancelButton);
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
                this.height / 2 - 32,
                0xFFFFFFFF
        );

        drawWrappedCenteredText(
                graphics,
                message.getString(),
                this.width / 2,
                this.height / 2 - 10,
                Math.max(120, this.width - 40),
                0xFFCCCCCC
        );

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawWrappedCenteredText(
            GuiGraphics graphics,
            String text,
            int centerX,
            int y,
            int maxWidth,
            int color
    ) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = y;

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;

            if (this.font.width(candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }

            if (!line.isEmpty()) {
                graphics.drawCenteredString(this.font, line.toString(), centerX, lineY, color);
                lineY += this.font.lineHeight + 2;
            }

            line.setLength(0);
            line.append(word);
        }

        if (!line.isEmpty()) {
            graphics.drawCenteredString(this.font, line.toString(), centerX, lineY, color);
        }
    }
}