package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

public class OverlayGroupSettingsScreen extends Screen {
    private static final TextColor GREEN = TextColor.fromRgb(0x55FF55);
    private static final TextColor RED = TextColor.fromRgb(0xFF5555);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    private static final int BUTTON_WIDTH = 200;
    private static final int SLIDER_WIDTH = 144;
    private static final int RESET_WIDTH = 52;
    private static final int COLOR_BUTTON_WIDTH = 24;
    private static final int COLOR_PREVIEW_WIDTH = 22;
    private static final int COLOR_ICON_DRAW_SIZE = 14;

    private static final int DEFAULT_PADDING = 8;
    private static final int DEFAULT_LINE_GAP = 3;
    private static final int DEFAULT_SCALE = 100;

    private static final Identifier COLOR_PALETTE_TEXTURE = Identifier.fromNamespaceAndPath(
            "pvp-overlay",
            "textures/gui/config-button-color-palette.png"
    );

    private final Screen parent;
    private final String groupId;

    private Button backgroundColorButton;
    private Button borderColorButton;

    public OverlayGroupSettingsScreen(Screen parent, String groupId) {
        super(Component.translatable("screen.pvp-overlay.group_settings.title"));
        this.parent = parent;
        this.groupId = groupId;
    }

    @Override
    protected void init() {
        PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);

        if (group == null) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }

        int centerX = this.width / 2;
        int leftX = centerX - 205;
        int rightX = centerX + 5;
        int startY = 58;
        int row = 28;

        Button showBoxButton = Button.builder(
                showBoxText(group),
                button -> {
                    PvPOverlayConfig.OverlayGroupConfig current = TestModClient.findOverlayGroup(groupId);

                    if (current != null) {
                        TestModClient.setGroupShowBox(groupId, !current.showBox);
                        button.setMessage(showBoxText(current));
                    }
                }
        ).bounds(leftX, startY, BUTTON_WIDTH, 20).build();

        Button showBorderButton = Button.builder(
                showBorderText(group),
                button -> {
                    PvPOverlayConfig.OverlayGroupConfig current = TestModClient.findOverlayGroup(groupId);

                    if (current != null) {
                        TestModClient.setGroupShowBorder(groupId, !current.showBorder);
                        button.setMessage(showBorderText(current));
                    }
                }
        ).bounds(leftX, startY + row, BUTTON_WIDTH, 20).build();

        Button hideOnTabButton = Button.builder(
                hideOnTabText(group),
                button -> {
                    PvPOverlayConfig.OverlayGroupConfig current = TestModClient.findOverlayGroup(groupId);

                    if (current != null) {
                        TestModClient.setGroupHideWhenPlayerListOpen(groupId, !current.hideWhenPlayerListOpen);
                        button.setMessage(hideOnTabText(current));
                    }
                }
        ).bounds(leftX, startY + row * 2, BUTTON_WIDTH, 20).build();

        int colorPreviewX = leftX + 120;
        int colorButtonX = colorPreviewX + COLOR_PREVIEW_WIDTH + 4;

        backgroundColorButton = Button.builder(
                Component.literal(" "),
                button -> {
                    PvPOverlayConfig.OverlayGroupConfig current = TestModClient.findOverlayGroup(groupId);

                    if (current != null) {
                        Minecraft.getInstance().setScreen(new ColorPickerScreen(
                                this,
                                Component.translatable("screen.pvp-overlay.group_background_color"),
                                current.backgroundColor,
                                current.backgroundOpacityPercent,
                                (color, alpha) -> {
                                    TestModClient.setGroupBackgroundColor(groupId, color);
                                    TestModClient.setGroupBackgroundOpacityPercent(groupId, alpha);
                                }
                        ));
                    }
                }
        ).bounds(colorButtonX, startY + row * 3, COLOR_BUTTON_WIDTH, 20).build();

        borderColorButton = Button.builder(
                Component.literal(" "),
                button -> {
                    PvPOverlayConfig.OverlayGroupConfig current = TestModClient.findOverlayGroup(groupId);

                    if (current != null) {
                        Minecraft.getInstance().setScreen(new ColorPickerScreen(
                                this,
                                Component.translatable("screen.pvp-overlay.group_border_color"),
                                current.borderColor,
                                current.borderOpacityPercent,
                                (color, alpha) -> {
                                    TestModClient.setGroupBorderColor(groupId, color);
                                    TestModClient.setGroupBorderOpacityPercent(groupId, alpha);
                                }
                        ));
                    }
                }
        ).bounds(colorButtonX, startY + row * 4, COLOR_BUTTON_WIDTH, 20).build();

        GroupIntSlider paddingSlider = new GroupIntSlider(
                rightX,
                startY,
                SLIDER_WIDTH,
                20,
                groupId,
                "padding"
        );

        Button resetPaddingButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> Minecraft.getInstance().setScreen(new ConfirmActionScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.confirm_reset.title"),
                        Component.translatable("screen.pvp-overlay.confirm_reset.padding"),
                        () -> {
                            TestModClient.setGroupPadding(groupId, DEFAULT_PADDING);
                            this.rebuildWidgets();
                        }
                ))
        ).bounds(rightX + SLIDER_WIDTH + 4, startY, RESET_WIDTH, 20).build();

        GroupIntSlider lineGapSlider = new GroupIntSlider(
                rightX,
                startY + row,
                SLIDER_WIDTH,
                20,
                groupId,
                "line_gap"
        );

        Button resetLineGapButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> Minecraft.getInstance().setScreen(new ConfirmActionScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.confirm_reset.title"),
                        Component.translatable("screen.pvp-overlay.confirm_reset.line_gap"),
                        () -> {
                            TestModClient.setGroupLineGap(groupId, DEFAULT_LINE_GAP);
                            this.rebuildWidgets();
                        }
                ))
        ).bounds(rightX + SLIDER_WIDTH + 4, startY + row, RESET_WIDTH, 20).build();

        GroupIntSlider scaleSlider = new GroupIntSlider(
                rightX,
                startY + row * 2,
                SLIDER_WIDTH,
                20,
                groupId,
                "scale"
        );

        Button resetScaleButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> Minecraft.getInstance().setScreen(new ConfirmActionScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.confirm_reset.title"),
                        Component.translatable("screen.pvp-overlay.confirm_reset.scale"),
                        () -> {
                            TestModClient.setGroupScalePercent(groupId, DEFAULT_SCALE);
                            this.rebuildWidgets();
                        }
                ))
        ).bounds(rightX + SLIDER_WIDTH + 4, startY + row * 2, RESET_WIDTH, 20).build();

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX - BUTTON_WIDTH / 2, this.height - 28, BUTTON_WIDTH, 20).build();

        this.addRenderableWidget(showBoxButton);
        this.addRenderableWidget(showBorderButton);
        this.addRenderableWidget(hideOnTabButton);

        this.addRenderableWidget(backgroundColorButton);
        this.addRenderableWidget(borderColorButton);

        this.addRenderableWidget(paddingSlider);
        this.addRenderableWidget(resetPaddingButton);

        this.addRenderableWidget(lineGapSlider);
        this.addRenderableWidget(resetLineGapButton);

        this.addRenderableWidget(scaleSlider);
        this.addRenderableWidget(resetScaleButton);

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
                Component.translatable("screen.pvp-overlay.group_settings.description"),
                this.width / 2,
                36,
                0xFFAAAAAA
        );

        drawColorRows(graphics);

        super.render(graphics, mouseX, mouseY, delta);

        drawPaletteIconIfVisible(graphics, backgroundColorButton);
        drawPaletteIconIfVisible(graphics, borderColorButton);
    }

    private void drawColorRows(GuiGraphics graphics) {
        PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);

        if (group == null) {
            return;
        }

        int centerX = this.width / 2;
        int leftX = centerX - 205;
        int startY = 58;
        int row = 28;

        int labelX = leftX;
        int previewX = leftX + 120;

        drawColorRow(
                graphics,
                Component.translatable("button.pvp-overlay.group_background_color"),
                group.backgroundColor,
                group.backgroundOpacityPercent,
                labelX,
                previewX,
                startY + row * 3
        );

        drawColorRow(
                graphics,
                Component.translatable("button.pvp-overlay.group_border_color"),
                group.borderColor,
                group.borderOpacityPercent,
                labelX,
                previewX,
                startY + row * 4
        );
    }

    private void drawColorRow(
            GuiGraphics graphics,
            Component label,
            String colorHex,
            int opacityPercent,
            int labelX,
            int previewX,
            int y
    ) {
        int color = PvPOverlayConfig.colorWithOpacity(colorHex, opacityPercent, 0xFFFFFFFF);

        graphics.drawString(
                this.font,
                label,
                labelX,
                y + 6,
                0xFFFFFFFF,
                false
        );

        graphics.fill(previewX, y + 2, previewX + COLOR_PREVIEW_WIDTH, y + 18, 0xFF000000);
        graphics.renderOutline(previewX, y + 2, COLOR_PREVIEW_WIDTH, 16, 0xFFFFFFFF);
        graphics.fill(previewX + 2, y + 4, previewX + COLOR_PREVIEW_WIDTH - 2, y + 16, color);
    }

    private void drawPaletteIconIfVisible(GuiGraphics graphics, Button button) {
        if (button == null || !button.visible) {
            return;
        }

        int startX = button.getX() + (button.getWidth() - COLOR_ICON_DRAW_SIZE) / 2;
        int startY = button.getY() + (button.getHeight() - COLOR_ICON_DRAW_SIZE) / 2;

        int iconColor = getButtonIconColor(button);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                COLOR_PALETTE_TEXTURE,
                startX,
                startY,
                0.0F,
                0.0F,
                COLOR_ICON_DRAW_SIZE,
                COLOR_ICON_DRAW_SIZE,
                COLOR_ICON_DRAW_SIZE,
                COLOR_ICON_DRAW_SIZE,
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

    private Component showBoxText(PvPOverlayConfig.OverlayGroupConfig group) {
        return settingText("button.pvp-overlay.group_show_box", group.showBox);
    }

    private Component showBorderText(PvPOverlayConfig.OverlayGroupConfig group) {
        return settingText("button.pvp-overlay.group_show_border", group.showBorder);
    }

    private Component hideOnTabText(PvPOverlayConfig.OverlayGroupConfig group) {
        return settingText("button.pvp-overlay.group_hide_on_tab", group.hideWhenPlayerListOpen);
    }

    private Component settingText(String labelKey, boolean value) {
        MutableComponent component = Component.translatable(labelKey)
                .append(": ")
                .withStyle(Style.EMPTY.withColor(WHITE));

        MutableComponent state = Component.translatable(value ? "state.pvp-overlay.on" : "state.pvp-overlay.off")
                .withStyle(Style.EMPTY.withColor(value ? GREEN : RED));

        return component.append(state);
    }

    private static class GroupIntSlider extends AbstractSliderButton {
        private final String groupId;
        private final String type;

        public GroupIntSlider(int x, int y, int width, int height, String groupId, String type) {
            super(
                    x,
                    y,
                    width,
                    height,
                    text(groupId, type),
                    value(groupId, type)
            );

            this.groupId = groupId;
            this.type = type;
        }

        @Override
        protected void updateMessage() {
            this.setMessage(textFromRaw(type, rawValue()));
        }

        @Override
        protected void applyValue() {
            int value = rawValue();

            switch (type) {
                case "padding" -> TestModClient.setGroupPadding(groupId, value);
                case "line_gap" -> TestModClient.setGroupLineGap(groupId, value);
                case "scale" -> TestModClient.setGroupScalePercent(groupId, value);
            }
        }

        private int rawValue() {
            return switch (type) {
                case "padding" -> (int) Math.round(this.value * 32.0);
                case "line_gap" -> (int) Math.round(this.value * 20.0);
                case "scale" -> 50 + (int) Math.round(this.value * 150.0);
                default -> 0;
            };
        }

        private static Component text(String groupId, String type) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);

            int value = switch (type) {
                case "padding" -> group == null ? DEFAULT_PADDING : group.paddingX;
                case "line_gap" -> group == null ? DEFAULT_LINE_GAP : group.lineGap;
                case "scale" -> group == null ? DEFAULT_SCALE : group.scalePercent;
                default -> 0;
            };

            return textFromRaw(type, value);
        }

        private static Component textFromRaw(String type, int value) {
            return switch (type) {
                case "padding" -> Component.translatable("slider.pvp-overlay.group_padding", value);
                case "line_gap" -> Component.translatable("slider.pvp-overlay.group_line_gap", value);
                case "scale" -> Component.translatable("slider.pvp-overlay.group_scale", value);
                default -> Component.literal(String.valueOf(value));
            };
        }

        private static double value(String groupId, String type) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);

            if (group == null) {
                return 0.0;
            }

            return switch (type) {
                case "padding" -> group.paddingX / 32.0;
                case "line_gap" -> group.lineGap / 20.0;
                case "scale" -> (group.scalePercent - 50) / 150.0;
                default -> 0.0;
            };
        }
    }
}