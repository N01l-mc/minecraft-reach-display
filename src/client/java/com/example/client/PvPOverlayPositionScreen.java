package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PvPOverlayPositionScreen extends Screen {
    private final Screen parent;

    private boolean wasMouseDown = false;
    private boolean dragging = false;

    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public PvPOverlayPositionScreen(Screen parent) {
        super(Component.literal("Move PvP Overlay"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        Button resetButton = Button.builder(
                Component.literal("Reset Position"),
                button -> TestModClient.resetOverlayPosition()
        ).bounds(centerX - 100, this.height - 56, 200, 20).build();

        Button doneButton = Button.builder(
                Component.literal("Done"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX - 100, this.height - 28, 200, 20).build();

        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(doneButton);
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
        Minecraft minecraft = Minecraft.getInstance();

        handleDragging(minecraft, mouseX, mouseY);

        graphics.fill(0, 0, this.width, this.height, TestModClient.getConfigMenuBackgroundColor());

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                20,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                "Drag the overlay box with left mouse button.",
                this.width / 2,
                38,
                0xFFAAAAAA
        );

        TestModClient.drawOverlay(graphics, minecraft);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void handleDragging(Minecraft minecraft, int mouseX, int mouseY) {
        long windowHandle = GLFW.glfwGetCurrentContext();

        if (windowHandle == 0L) {
            return;
        }

        boolean mouseDown = GLFW.glfwGetMouseButton(
                windowHandle,
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;

        int boxX = TestModClient.getOverlayX(minecraft);
        int boxY = TestModClient.getOverlayY(minecraft);
        int boxWidth = TestModClient.getOverlayBoxWidth(minecraft);
        int boxHeight = TestModClient.getOverlayBoxHeight(minecraft);

        boolean mouseInsideBox =
                mouseX >= boxX &&
                        mouseX <= boxX + boxWidth &&
                        mouseY >= boxY &&
                        mouseY <= boxY + boxHeight;

        if (mouseDown && !wasMouseDown && mouseInsideBox) {
            dragging = true;
            dragOffsetX = mouseX - boxX;
            dragOffsetY = mouseY - boxY;
        }

        if (!mouseDown) {
            dragging = false;
        }

        if (dragging) {
            TestModClient.setOverlayPosition(
                    mouseX - dragOffsetX,
                    mouseY - dragOffsetY
            );
        }

        wasMouseDown = mouseDown;
    }
}