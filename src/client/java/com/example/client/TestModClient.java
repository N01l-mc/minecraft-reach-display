package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public class TestModClient implements ClientModInitializer {
	private static String hitMainText = "-";
	private static String hitRoundedDigit = "";

	private static String takenMainText = "-";
	private static String takenRoundedDigit = "";

	@Override
	public void onInitializeClient() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft minecraft = Minecraft.getInstance();

			String hitPrefix = "Hit: ";
			String takenPrefix = "Taken: ";
			String suffix = " blocks";

			String hitFullText = hitPrefix + hitMainText + hitRoundedDigit + suffix;
			String takenFullText = takenPrefix + takenMainText + takenRoundedDigit + suffix;

			int paddingX = 8;
			int paddingY = 8;
			int lineHeight = 12;

			int hitWidth = minecraft.font.width(hitFullText);
			int takenWidth = minecraft.font.width(takenFullText);

			int textWidth = Math.max(hitWidth, takenWidth);
			int boxWidth = textWidth + paddingX * 2;
			int boxHeight = 36;

			int screenWidth = minecraft.getWindow().getGuiScaledWidth();

			int x = (screenWidth - boxWidth) / 2;
			int y = 10;

			graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xAA000000);
			graphics.renderOutline(x, y, boxWidth, boxHeight, 0xFFFFFFFF);

			int textX = x + paddingX;
			int textY = y + paddingY;

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

			drawDistanceLine(
					graphics,
					minecraft,
					textX,
					textY + lineHeight,
					takenPrefix,
					takenMainText,
					takenRoundedDigit,
					suffix
			);
		});
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