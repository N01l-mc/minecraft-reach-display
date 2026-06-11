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

public class OverlayGroupSettingsScreen extends Screen {
    private static final TextColor GREEN = TextColor.fromRgb(0x55FF55);
    private static final TextColor RED = TextColor.fromRgb(0xFF5555);
    private static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

    private static final int BUTTON_WIDTH = 200;
    private static final int SLIDER_WIDTH = 144;
    private static final int RESET_WIDTH = 52;

    private final Screen parent;
    private final String groupId;

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

        Button backgroundColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.group_background_color"),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.group_background_color"),
                        TestModClient.findOverlayGroup(groupId).backgroundColor,
                        value -> TestModClient.setGroupBackgroundColor(groupId, value)
                ))
        ).bounds(leftX, startY + row * 3, BUTTON_WIDTH, 20).build();

        Button borderColorButton = Button.builder(
                Component.translatable("button.pvp-overlay.group_border_color"),
                button -> Minecraft.getInstance().setScreen(new ColorPickerScreen(
                        this,
                        Component.translatable("screen.pvp-overlay.group_border_color"),
                        TestModClient.findOverlayGroup(groupId).borderColor,
                        value -> TestModClient.setGroupBorderColor(groupId, value)
                ))
        ).bounds(leftX, startY + row * 4, BUTTON_WIDTH, 20).build();

        GroupOpacitySlider backgroundOpacitySlider = new GroupOpacitySlider(
                rightX,
                startY,
                SLIDER_WIDTH,
                20,
                groupId,
                true
        );

        Button resetBackgroundOpacityButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.setGroupBackgroundOpacityPercent(groupId, 67);
                    this.rebuildWidgets();
                }
        ).bounds(rightX + SLIDER_WIDTH + 4, startY, RESET_WIDTH, 20).build();

        GroupOpacitySlider borderOpacitySlider = new GroupOpacitySlider(
                rightX,
                startY + row,
                SLIDER_WIDTH,
                20,
                groupId,
                false
        );

        Button resetBorderOpacityButton = Button.builder(
                Component.translatable("button.pvp-overlay.reset"),
                button -> {
                    TestModClient.setGroupBorderOpacityPercent(groupId, 100);
                    this.rebuildWidgets();
                }
        ).bounds(rightX + SLIDER_WIDTH + 4, startY + row, RESET_WIDTH, 20).build();

        GroupIntSlider paddingXSlider = new GroupIntSlider(
                rightX,
                startY + row * 2,
                SLIDER_WIDTH,
                20,
                groupId,
                "padding_x"
        );

        GroupIntSlider paddingYSlider = new GroupIntSlider(
                rightX,
                startY + row * 3,
                SLIDER_WIDTH,
                20,
                groupId,
                "padding_y"
        );

        GroupIntSlider lineGapSlider = new GroupIntSlider(
                rightX,
                startY + row * 4,
                SLIDER_WIDTH,
                20,
                groupId,
                "line_gap"
        );

        GroupIntSlider scaleSlider = new GroupIntSlider(
                centerX - SLIDER_WIDTH / 2,
                startY + row * 6,
                SLIDER_WIDTH,
                20,
                groupId,
                "scale"
        );

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(centerX - BUTTON_WIDTH / 2, this.height - 28, BUTTON_WIDTH, 20).build();

        this.addRenderableWidget(showBoxButton);
        this.addRenderableWidget(showBorderButton);
        this.addRenderableWidget(hideOnTabButton);
        this.addRenderableWidget(backgroundColorButton);
        this.addRenderableWidget(borderColorButton);

        this.addRenderableWidget(backgroundOpacitySlider);
        this.addRenderableWidget(resetBackgroundOpacityButton);
        this.addRenderableWidget(borderOpacitySlider);
        this.addRenderableWidget(resetBorderOpacityButton);

        this.addRenderableWidget(paddingXSlider);
        this.addRenderableWidget(paddingYSlider);
        this.addRenderableWidget(lineGapSlider);
        this.addRenderableWidget(scaleSlider);

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

        super.render(graphics, mouseX, mouseY, delta);
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

    private static class GroupOpacitySlider extends AbstractSliderButton {
        private final String groupId;
        private final boolean background;

        public GroupOpacitySlider(int x, int y, int width, int height, String groupId, boolean background) {
            super(
                    x,
                    y,
                    width,
                    height,
                    text(groupId, background),
                    value(groupId, background)
            );

            this.groupId = groupId;
            this.background = background;
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0);

            this.setMessage(Component.translatable(
                    background ? "slider.pvp-overlay.group_background_opacity" : "slider.pvp-overlay.group_border_opacity",
                    percent
            ));
        }

        @Override
        protected void applyValue() {
            int percent = (int) Math.round(this.value * 100.0);

            if (background) {
                TestModClient.setGroupBackgroundOpacityPercent(groupId, percent);
            } else {
                TestModClient.setGroupBorderOpacityPercent(groupId, percent);
            }
        }

        private static Component text(String groupId, boolean background) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);
            int percent = 100;

            if (group != null) {
                percent = background ? group.backgroundOpacityPercent : group.borderOpacityPercent;
            }

            return Component.translatable(
                    background ? "slider.pvp-overlay.group_background_opacity" : "slider.pvp-overlay.group_border_opacity",
                    percent
            );
        }

        private static double value(String groupId, boolean background) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);

            if (group == null) {
                return 1.0;
            }

            return (background ? group.backgroundOpacityPercent : group.borderOpacityPercent) / 100.0;
        }
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
                case "padding_x" -> TestModClient.setGroupPaddingX(groupId, value);
                case "padding_y" -> TestModClient.setGroupPaddingY(groupId, value);
                case "line_gap" -> TestModClient.setGroupLineGap(groupId, value);
                case "scale" -> TestModClient.setGroupScalePercent(groupId, value);
            }
        }

        private int rawValue() {
            return switch (type) {
                case "padding_x", "padding_y" -> (int) Math.round(this.value * 32.0);
                case "line_gap" -> (int) Math.round(this.value * 20.0);
                case "scale" -> 50 + (int) Math.round(this.value * 150.0);
                default -> 0;
            };
        }

        private static Component text(String groupId, String type) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);

            int value = switch (type) {
                case "padding_x" -> group == null ? 8 : group.paddingX;
                case "padding_y" -> group == null ? 8 : group.paddingY;
                case "line_gap" -> group == null ? 3 : group.lineGap;
                case "scale" -> group == null ? 100 : group.scalePercent;
                default -> 0;
            };

            return textFromRaw(type, value);
        }

        private static Component textFromRaw(String type, int value) {
            return switch (type) {
                case "padding_x" -> Component.translatable("slider.pvp-overlay.group_padding_x", value);
                case "padding_y" -> Component.translatable("slider.pvp-overlay.group_padding_y", value);
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
                case "padding_x" -> group.paddingX / 32.0;
                case "padding_y" -> group.paddingY / 32.0;
                case "line_gap" -> group.lineGap / 20.0;
                case "scale" -> (group.scalePercent - 50) / 150.0;
                default -> 0.0;
            };
        }
    }
}