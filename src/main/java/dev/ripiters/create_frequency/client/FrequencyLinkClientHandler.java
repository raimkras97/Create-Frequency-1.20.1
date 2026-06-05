package dev.ripiters.create_frequency.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import dev.ripiters.create_frequency.CreateFrequency;
import dev.ripiters.create_frequency.common.link.FrequencyBlockEntity;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FrequencyLinkClientHandler {

    private static final String OVERLAY_ID = CreateFrequency.MODID + ".frequency_link.overlay";

    private FrequencyLinkClientHandler() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.options.hideGui) {
            return;
        }

        HitResult hitResult = mc.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (!(mc.level.getBlockEntity(pos) instanceof FrequencyBlockEntity be)) {
            return;
        }

        Direction face = blockHitResult.getDirection();
        
        // Only show on top face
        if (face != Direction.UP) {
            return;
        }

        FrequencyValueBoxTransform transform = new FrequencyValueBoxTransform();

        Vec3 localHit = blockHitResult.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        if (!transform.testHit(mc.level, pos, be.getBlockState(), localHit)) {
            return;
        }

        // Small square area like pulse repeater
        AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.125f);
        
        ValueBox box = new ValueBox.TextValueBox(
                Component.empty(),
                bb,
                pos,
                be.getBlockState(),
                Component.literal(be.getFormattedFrequency())
        );
        box.withColor(0xFFFFFF); // White outline

        Outliner.getInstance()
                .showOutline(OVERLAY_ID + pos.asLong(), box.transform(transform))
                .lineWidth(1/16f);
    }

    // Custom transform for frequency link - positioned like pulse repeater
    private static class FrequencyValueBoxTransform extends ValueBoxTransform {

        @Override
        public Vec3 getLocalOffset(net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockState state) {
            // Position at center top, slightly above surface (like pulse repeater)
            // voxelSpace(8, 16.5, 8) = (8/16, 16.5/16, 8/16) = (0.5, 1.03125, 0.5)
            return new Vec3(0.5, 1.03125, 0.5);
        }

        @Override
        public void rotate(net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockState state, PoseStack ms) {
            // No rotation needed for top face
        }

        public boolean testHit(net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockState state, Vec3 localHit) {
            // Test if hit is on top face and in center area
            if (localHit.y < 0.95) return false;
            
            double dx = localHit.x - 0.5;
            double dz = localHit.z - 0.5;
            return dx * dx + dz * dz < 0.15 * 0.15; // Small radius around center
        }
    }
}
