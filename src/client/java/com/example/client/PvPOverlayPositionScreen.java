package com.example.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class PvPOverlayPositionScreen extends Screen {
    private static final int SNAP_DISTANCE = 8;
    private static final int CONTEXT_ITEM_HEIGHT = 18;
    private static final int CONTEXT_WIDTH = 150;

    private static final int DONE_BUTTON_WIDTH = 200;
    private static final int UNDO_BUTTON_WIDTH = 96;
    private static final int BOTTOM_BUTTON_HEIGHT = 20;
    private static final int BOTTOM_MARGIN = 24;
    private static final int CORNER_MARGIN = 8;

    private final Screen parent;

    private String draggingGroupId = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    private String hoveredGroupId = null;
    private String hoveredModuleId = null;

    private String targetGroupId = null;
    private int targetInsertIndex = 0;

    private ContextMenu contextMenu = null;

    private final ArrayDeque<String> undoStack = new ArrayDeque<>();

    public PvPOverlayPositionScreen(Screen parent) {
        super(Component.translatable("screen.pvp-overlay.position.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        Button doneButton = Button.builder(
                Component.translatable("button.pvp-overlay.done"),
                button -> Minecraft.getInstance().setScreen(parent)
        ).bounds(
                this.width / 2 - DONE_BUTTON_WIDTH / 2,
                this.height - BOTTOM_MARGIN,
                DONE_BUTTON_WIDTH,
                BOTTOM_BUTTON_HEIGHT
        ).build();

        Button undoButton = Button.builder(
                Component.translatable("button.pvp-overlay.undo"),
                button -> undoLastAction()
        ).bounds(
                this.width - UNDO_BUTTON_WIDTH - CORNER_MARGIN,
                this.height - BOTTOM_MARGIN,
                UNDO_BUTTON_WIDTH,
                BOTTOM_BUTTON_HEIGHT
        ).build();

        this.addRenderableWidget(doneButton);
        this.addRenderableWidget(undoButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        TestModClient.drawConfigMenuBackdrop(graphics, this.width, this.height);

        updateHover(mouseX, mouseY);

        Minecraft minecraft = Minecraft.getInstance();

        findSnapTarget(mouseX, mouseY);

        for (PvPOverlayConfig.OverlayGroupConfig group : TestModClient.getOverlayGroups()) {
            boolean hovered = group.id.equals(hoveredGroupId);
            String hoveredModule = group.id.equals(hoveredGroupId) ? hoveredModuleId : null;

            TestModClient.drawGroupEditorPreview(graphics, minecraft, group, hovered, hoveredModule);

            if (group.id.equals(targetGroupId)) {
                TestModClient.OverlayGroupBounds bounds = TestModClient.getGroupBounds(minecraft, group);

                graphics.renderOutline(
                        bounds.x() - SNAP_DISTANCE,
                        bounds.y() - SNAP_DISTANCE,
                        bounds.width() + SNAP_DISTANCE * 2,
                        bounds.height() + SNAP_DISTANCE * 2,
                        0xFF55FF55
                );

                drawInsertLine(graphics, bounds, targetInsertIndex);
            }
        }

        graphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                10,
                0xFFFFFFFF
        );

        drawWrappedCenteredText(
                graphics,
                Component.translatable("screen.pvp-overlay.position.instructions").getString(),
                this.width / 2,
                24,
                Math.max(120, this.width - 24),
                0xFFAAAAAA
        );

        if (hoveredGroupId != null) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.pvp-overlay.position.right_click_options"),
                    this.width / 2,
                    this.height - 48,
                    0xFFFFFF55
            );
        }

        super.render(graphics, mouseX, mouseY, delta);

        if (contextMenu != null) {
            drawContextMenu(graphics, mouseX, mouseY);
        }
    }

    private void drawWrappedCenteredText(
            GuiGraphics graphics,
            String text,
            int centerX,
            int y,
            int maxWidth,
            int color
    ) {
        ArrayList<String> lines = wrapText(text, maxWidth);

        for (int i = 0; i < lines.size(); i++) {
            graphics.drawCenteredString(
                    this.font,
                    lines.get(i),
                    centerX,
                    y + i * (this.font.lineHeight + 2),
                    color
            );
        }
    }

    private ArrayList<String> wrapText(String text, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            String candidate = currentLine.isEmpty()
                    ? word
                    : currentLine + " " + word;

            if (this.font.width(candidate) <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
            }

            if (this.font.width(word) <= maxWidth) {
                currentLine.append(word);
            } else {
                lines.add(word);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void drawInsertLine(GuiGraphics graphics, TestModClient.OverlayGroupBounds bounds, int insertIndex) {
        int lineY;

        if (bounds.rows().isEmpty()) {
            lineY = bounds.y() + bounds.height() / 2;
        } else if (insertIndex <= 0) {
            TestModClient.OverlayModuleRow first = bounds.rows().get(0);
            lineY = first.y() - 4;
        } else if (insertIndex >= bounds.rows().size()) {
            TestModClient.OverlayModuleRow last = bounds.rows().get(bounds.rows().size() - 1);
            lineY = last.y() + last.height() + 4;
        } else {
            TestModClient.OverlayModuleRow row = bounds.rows().get(insertIndex);
            lineY = row.y() - 4;
        }

        graphics.fill(bounds.x() + 3, lineY, bounds.x() + bounds.width() - 3, lineY + 2, 0xFF55FF55);
    }

    private void updateHover(double mouseX, double mouseY) {
        hoveredGroupId = null;
        hoveredModuleId = null;

        Minecraft minecraft = Minecraft.getInstance();

        ArrayList<PvPOverlayConfig.OverlayGroupConfig> groups = TestModClient.getOverlayGroups();

        for (int i = groups.size() - 1; i >= 0; i--) {
            PvPOverlayConfig.OverlayGroupConfig group = groups.get(i);
            TestModClient.OverlayGroupBounds bounds = TestModClient.getGroupBounds(minecraft, group);

            if (!bounds.contains(mouseX, mouseY)) {
                continue;
            }

            hoveredGroupId = group.id;

            for (TestModClient.OverlayModuleRow row : bounds.rows()) {
                if (row.contains(mouseX, mouseY)) {
                    hoveredModuleId = row.moduleId();
                    break;
                }
            }

            return;
        }
    }

    private void findSnapTarget(double mouseX, double mouseY) {
        targetGroupId = null;
        targetInsertIndex = 0;

        if (draggingGroupId == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        for (PvPOverlayConfig.OverlayGroupConfig group : TestModClient.getOverlayGroups()) {
            if (group.id.equals(draggingGroupId)) {
                continue;
            }

            TestModClient.OverlayGroupBounds bounds = TestModClient.getGroupBounds(minecraft, group);

            boolean close =
                    mouseX >= bounds.x() - SNAP_DISTANCE &&
                            mouseX <= bounds.x() + bounds.width() + SNAP_DISTANCE &&
                            mouseY >= bounds.y() - SNAP_DISTANCE &&
                            mouseY <= bounds.y() + bounds.height() + SNAP_DISTANCE;

            if (close) {
                targetGroupId = group.id;
                targetInsertIndex = getInsertIndexForMouse(bounds, mouseY);
                return;
            }
        }
    }

    private int getInsertIndexForMouse(TestModClient.OverlayGroupBounds bounds, double mouseY) {
        if (bounds.rows().isEmpty()) {
            return 0;
        }

        for (int i = 0; i < bounds.rows().size(); i++) {
            TestModClient.OverlayModuleRow row = bounds.rows().get(i);
            int rowMiddle = row.y() + row.height() / 2;

            if (mouseY < rowMiddle) {
                return i;
            }
        }

        return bounds.rows().size();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (contextMenu != null) {
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (handleContextMenuClick(mouseX, mouseY)) {
                    return true;
                }

                contextMenu = null;
                return true;
            }

            contextMenu = null;
        }

        updateHover(mouseX, mouseY);

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (hoveredGroupId != null) {
                contextMenu = new ContextMenu(
                        (int) mouseX,
                        (int) mouseY,
                        hoveredGroupId,
                        hoveredModuleId
                );

                return true;
            }
        }

        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && hoveredGroupId != null) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(hoveredGroupId);

            if (group == null) {
                return true;
            }

            pushUndoSnapshot();

            draggingGroupId = hoveredGroupId;
            dragOffsetX = (int) mouseX - group.x;
            dragOffsetY = (int) mouseY - group.y;

            return true;
        }

        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dragX, double dragY) {
        if (draggingGroupId != null) {
            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(draggingGroupId);

            if (group != null) {
                group.x = (int) click.x() - dragOffsetX;
                group.y = (int) click.y() - dragOffsetY;
                TestModClient.clampGroupToScreen(Minecraft.getInstance(), group);
            }

            return true;
        }

        return super.mouseDragged(click, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (draggingGroupId != null) {
            if (targetGroupId != null) {
                PvPOverlayConfig.OverlayGroupConfig draggingGroup = TestModClient.findOverlayGroup(draggingGroupId);
                PvPOverlayConfig.OverlayGroupConfig targetGroup = TestModClient.findOverlayGroup(targetGroupId);

                if (draggingGroup != null && targetGroup != null && draggingGroup.modules != null) {
                    ArrayList<String> movedModules = new ArrayList<>(draggingGroup.modules);
                    int insertIndex = targetInsertIndex;

                    for (String moduleId : movedModules) {
                        TestModClient.moveModuleToGroup(moduleId, targetGroup.id, insertIndex);
                        insertIndex++;
                    }
                }
            } else {
                PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(draggingGroupId);

                if (group != null) {
                    TestModClient.clampGroupToScreen(Minecraft.getInstance(), group);
                }

                TestModClient.saveConfig();
            }

            draggingGroupId = null;
            targetGroupId = null;
            targetInsertIndex = 0;

            return true;
        }

        return super.mouseReleased(click);
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

    private void pushUndoSnapshot() {
        undoStack.push(TestModClient.createLayoutSnapshot());

        while (undoStack.size() > 20) {
            undoStack.removeLast();
        }
    }

    private void undoLastAction() {
        if (undoStack.isEmpty()) {
            return;
        }

        TestModClient.restoreLayoutSnapshot(undoStack.pop());
    }

    private void drawContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = contextMenu.x;
        int y = contextMenu.y;

        int height = contextMenu.getItemCount() * CONTEXT_ITEM_HEIGHT;

        graphics.fill(x, y, x + CONTEXT_WIDTH, y + height, 0xEE101010);
        graphics.renderOutline(x, y, CONTEXT_WIDTH, height, 0xFFFFFFFF);

        for (int i = 0; i < contextMenu.getItemCount(); i++) {
            int itemY = y + i * CONTEXT_ITEM_HEIGHT;
            boolean hovered = mouseX >= x &&
                    mouseX <= x + CONTEXT_WIDTH &&
                    mouseY >= itemY &&
                    mouseY <= itemY + CONTEXT_ITEM_HEIGHT;

            if (hovered) {
                graphics.fill(x + 1, itemY + 1, x + CONTEXT_WIDTH - 1, itemY + CONTEXT_ITEM_HEIGHT - 1, 0xAA3355AA);
            }

            graphics.drawString(
                    this.font,
                    contextMenu.getItemLabel(i),
                    x + 5,
                    itemY + 5,
                    0xFFFFFFFF,
                    false
            );
        }
    }

    private boolean handleContextMenuClick(double mouseX, double mouseY) {
        if (contextMenu == null) {
            return false;
        }

        int index = contextMenu.getItemIndex(mouseX, mouseY);

        if (index < 0) {
            return false;
        }

        String action = contextMenu.getAction(index);

        switch (action) {
            case "group_settings" -> Minecraft.getInstance().setScreen(new OverlayGroupSettingsScreen(this, contextMenu.groupId));
            case "ungroup_all" -> {
                pushUndoSnapshot();
                TestModClient.ungroupAll(contextMenu.groupId);
            }
            case "move_up" -> {
                pushUndoSnapshot();
                TestModClient.moveModuleUp(contextMenu.groupId, contextMenu.moduleId);
            }
            case "move_down" -> {
                pushUndoSnapshot();
                TestModClient.moveModuleDown(contextMenu.groupId, contextMenu.moduleId);
            }
            case "remove_module" -> {
                pushUndoSnapshot();
                TestModClient.removeModuleFromGroupToNewGroup(contextMenu.moduleId, contextMenu.groupId);
            }
            case "reset_layout" -> Minecraft.getInstance().setScreen(new ConfirmActionScreen(
                    this,
                    Component.translatable("screen.pvp-overlay.confirm_reset.title"),
                    Component.translatable("screen.pvp-overlay.confirm_reset.layout"),
                    () -> {
                        pushUndoSnapshot();
                        TestModClient.resetOverlayLayout();
                    }
            ));
        }

        contextMenu = null;
        return true;
    }

    private static class ContextMenu {
        private final int x;
        private final int y;
        private final String groupId;
        private final String moduleId;
        private final ArrayList<String> actions = new ArrayList<>();
        private final ArrayList<Component> labels = new ArrayList<>();

        private ContextMenu(int x, int y, String groupId, String moduleId) {
            this.x = x;
            this.y = y;
            this.groupId = groupId;
            this.moduleId = moduleId;

            PvPOverlayConfig.OverlayGroupConfig group = TestModClient.findOverlayGroup(groupId);
            int moduleCount = group == null || group.modules == null ? 0 : group.modules.size();

            actions.add("group_settings");
            labels.add(Component.translatable("menu.pvp-overlay.group_settings"));

            if (moduleId != null && moduleCount > 1) {
                actions.add("move_up");
                labels.add(Component.translatable("menu.pvp-overlay.move_up"));

                actions.add("move_down");
                labels.add(Component.translatable("menu.pvp-overlay.move_down"));

                actions.add("remove_module");
                labels.add(Component.translatable("menu.pvp-overlay.remove_from_group"));
            }

            if (moduleCount > 1) {
                actions.add("ungroup_all");
                labels.add(Component.translatable("menu.pvp-overlay.ungroup_all"));
            }

            actions.add("reset_layout");
            labels.add(Component.translatable("menu.pvp-overlay.reset_layout"));
        }

        private int getItemCount() {
            return actions.size();
        }

        private Component getItemLabel(int index) {
            return labels.get(index);
        }

        private String getAction(int index) {
            return actions.get(index);
        }

        private int getItemIndex(double mouseX, double mouseY) {
            if (mouseX < x || mouseX > x + CONTEXT_WIDTH) {
                return -1;
            }

            int index = (int) ((mouseY - y) / CONTEXT_ITEM_HEIGHT);

            if (index < 0 || index >= actions.size()) {
                return -1;
            }

            return index;
        }
    }
}