package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class TestModClient implements ClientModInitializer {
	private static final PvPOverlayConfig CONFIG = PvPOverlayConfig.load();

	private static String hitMainText = "-";
	private static String hitRoundedDigit = "";

	private static String takenMainText = "-";
	private static String takenRoundedDigit = "";

	private static KeyMapping openConfigKey;

	@Override
	public void onInitializeClient() {
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.pvp-overlay.open_config",
				GLFW.GLFW_KEY_O,
				KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openConfigKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new PvPOverlayConfigScreen());
				}
			}
		});

		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			if (!CONFIG.overlayEnabled) {
				return;
			}

			Minecraft minecraft = Minecraft.getInstance();

			String hitPrefix = "Hit: ";
			String takenPrefix = "Taken: ";
			String suffix = " blocks";

			String hitFullText = hitPrefix + hitMainText + hitRoundedDigit + suffix;
			String takenFullText = takenPrefix + takenMainText + takenRoundedDigit + suffix;

			int paddingX = 8;
			int paddingY = 8;
			int lineGap = 3;
			int fontHeight = minecraft.font.lineHeight;

			int visibleLines = 0;
			int textWidth = 0;

			if (CONFIG.showHit) {
				visibleLines++;
				textWidth = Math.max(textWidth, minecraft.font.width(hitFullText));
			}

			if (CONFIG.showTaken) {
				visibleLines++;
				textWidth = Math.max(textWidth, minecraft.font.width(takenFullText));
			}

			if (visibleLines == 0) {
				return;
			}

			int contentHeight = visibleLines * fontHeight + Math.max(0, visibleLines - 1) * lineGap;
			int boxWidth = textWidth + paddingX * 2;
			int boxHeight = contentHeight + paddingY * 2;

			int screenWidth = minecraft.getWindow().getGuiScaledWidth();

			int x = (screenWidth - boxWidth) / 2;
			int y = 10;

			graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xAA000000);
			graphics.renderOutline(x, y, boxWidth, boxHeight, 0xFFFFFFFF);

			int textX = x + paddingX;
			int textY = y + (boxHeight - contentHeight) / 2;

			if (CONFIG.showHit) {
				drawDistanceLine(
						graphics,
						minecraft,
						textX,
						textY,
						hitPrefix,
						hitMainText,
						hitRoundedDigit,
						suffix
				);

				textY += fontHeight + lineGap;
			}

			if (CONFIG.showTaken) {
				drawDistanceLine(
						graphics,
						minecraft,
						textX,
						textY,
						takenPrefix,
						takenMainText,
						takenRoundedDigit,
						suffix
				);
			}
		});
	}

	public static boolean isOpenConfigKey(KeyEvent input) {
		return openConfigKey != null && openConfigKey.matches(input);
	}

	public static void recordOutgoingHit(Player player, Entity target) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || player != minecraft.player) {
			return;
		}

		if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
			return;
		}

		if (entityHitResult.getEntity() != target) {
			return;
		}

		Vec3 eyePosition = player.getEyePosition();
		Vec3 hitPosition = entityHitResult.getLocation();

		double distance = eyePosition.distanceTo(hitPosition);

		setHitDistance(distance);
	}

	public static void recordIncomingHit(Player hurtEntity, DamageSource damageSource) {
		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || hurtEntity != minecraft.player) {
			return;
		}

		Entity attacker = damageSource.getEntity();

		if (attacker == null) {
			return;
		}

		Vec3 attackerEyes = attacker.getEyePosition();
		AABB myHitbox = minecraft.player.getBoundingBox();

		Vec3 closestPointOnMyHitbox = getClosestPointOnBox(attackerEyes, myHitbox);

		double distance = attackerEyes.distanceTo(closestPointOnMyHitbox);

		setTakenDistance(distance);
	}

	public static boolean isOverlayEnabled() {
		return CONFIG.overlayEnabled;
	}

	public static boolean isShowHit() {
		return CONFIG.showHit;
	}

	public static boolean isShowTaken() {
		return CONFIG.showTaken;
	}

	public static void toggleOverlayEnabled() {
		CONFIG.overlayEnabled = !CONFIG.overlayEnabled;
		CONFIG.save();
	}

	public static void toggleShowHit() {
		CONFIG.showHit = !CONFIG.showHit;
		CONFIG.save();
	}

	public static void toggleShowTaken() {
		CONFIG.showTaken = !CONFIG.showTaken;
		CONFIG.save();
	}

	private static void setHitDistance(double distance) {
		String[] parts = formatDistance(distance);
		hitMainText = parts[0];
		hitRoundedDigit = parts[1];
	}

	private static void setTakenDistance(double distance) {
		String[] parts = formatDistance(distance);
		takenMainText = parts[0];
		takenRoundedDigit = parts[1];
	}

	private static String[] formatDistance(double distance) {
		String formattedDistance = String.format(Locale.US, "%.4f", distance);

		if (formattedDistance.contains(".")) {
			int dotIndex = formattedDistance.indexOf(".");
			int splitIndex = Math.min(dotIndex + 4, formattedDistance.length());

			String mainText = formattedDistance.substring(0, splitIndex);
			String roundedDigit = formattedDistance.substring(splitIndex);

			return new String[]{mainText, roundedDigit};
		}

		return new String[]{formattedDistance, ""};
	}

	private static Vec3 getClosestPointOnBox(Vec3 point, AABB box) {
		double x = clamp(point.x, box.minX, box.maxX);
		double y = clamp(point.y, box.minY, box.maxY);
		double z = clamp(point.z, box.minZ, box.maxZ);

		return new Vec3(x, y, z);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	private static void drawDistanceLine(
			net.minecraft.client.gui.GuiGraphics graphics,
			Minecraft minecraft,
			int x,
			int y,
			String prefix,
			String mainText,
			String roundedDigit,
			String suffix
	) {
		int textX = x;

		graphics.drawString(
				minecraft.font,
				prefix,
				textX,
				y,
				0xFFFFFFFF,
				false
		);

		textX += minecraft.font.width(prefix);

		graphics.drawString(
				minecraft.font,
				mainText,
				textX,
				y,
				0xFFFFFFFF,
				false
		);

		textX += minecraft.font.width(mainText);

		graphics.drawString(
				minecraft.font,
				roundedDigit,
				textX,
				y,
				0xFF888888,
				false
		);

		textX += minecraft.font.width(roundedDigit);

		graphics.drawString(
				minecraft.font,
				suffix,
				textX,
				y,
				0xFFFFFFFF,
				false
		);
	}
}