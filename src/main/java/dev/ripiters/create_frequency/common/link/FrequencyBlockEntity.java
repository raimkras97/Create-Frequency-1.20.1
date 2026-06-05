package dev.ripiters.create_frequency.common.link;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.ripiters.create_frequency.common.CFLang;
import net.minecraft.ChatFormatting;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ripiters.create_frequency.common.network.FrequencyNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public abstract class FrequencyBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected FrequencyLinkBehaviour link;
    protected String networkName = "";
    private static final String KEY_TOOLTIP_TYPE = "tooltip.frequency_link.type";
    private static final String KEY_TOOLTIP_MODE_TRANSMITTER = "tooltip.frequency_link.mode.transmitter";
    private static final String KEY_TOOLTIP_MODE_RECEIVER = "tooltip.frequency_link.mode.receiver";
    private static final String KEY_TOOLTIP_FREQUENCY = "tooltip.frequency_link.frequency";
    private static final String KEY_TOOLTIP_NETWORK = "tooltip.frequency_link.network";

    public FrequencyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && level.getGameTime() % 20 == 0) {
            refreshNetworkData();
        }
    }

    public void refreshNetworkData() {
        if (level == null || level.isClientSide) return;

        String globalName = FrequencyNetworkHandler.getNetworkName(level, getFrequency());

        if (globalName.isEmpty() && !this.networkName.isEmpty()) {
            this.networkName = "";
            sendData();
            setChanged();
        }
        else if (!globalName.isEmpty() && !globalName.equals(this.networkName)) {
            this.networkName = globalName;
            sendData();
            setChanged();
        }
    }

    public void setFrequency(float freq) {
        if (link != null) {
            link.setFrequency(freq);
            refreshNetworkData();
        }
    }

    public void setNetworkName(String name) {
        this.networkName = (name == null || name.isEmpty()) ? "" : name;

        if (level != null && !level.isClientSide) {
            FrequencyNetworkHandler.setNetworkName(level, getFrequency(), this.networkName);
            sendData();
            setChanged();
        }
    }

    public float getFrequency() {
        return link != null ? link.getNetworkKey() : 0.0f;
    }

    public String getNetworkName() {
        return networkName;
    }

    public boolean isTransmitter() {
        return this instanceof FrequencyTransmitterBlockEntity;
    }

    public String getFormattedFrequency() {
        return formatFreq(getFrequency()) + "Hz";
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(CFLang.translateDirect(KEY_TOOLTIP_TYPE, getTypeComponent()).withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
        CreateLang.builder()
                .add(CFLang.translateDirect(KEY_TOOLTIP_FREQUENCY,
                        Component.literal(getFormattedFrequency()).withStyle(ChatFormatting.GOLD))
                        .withStyle(ChatFormatting.GRAY))
                .forGoggles(tooltip);
        if (!networkName.isBlank()) {
            CreateLang.builder()
                    .add(CFLang.translateDirect(KEY_TOOLTIP_NETWORK,
                            Component.literal(networkName).withStyle(ChatFormatting.WHITE))
                            .withStyle(ChatFormatting.GRAY))
                    .forGoggles(tooltip);
        }
        return true;
    }

    private Component getTypeComponent() {
        return CFLang.translateDirect(isTransmitter() ? KEY_TOOLTIP_MODE_TRANSMITTER : KEY_TOOLTIP_MODE_RECEIVER)
                .withStyle(ChatFormatting.GOLD);
    }

    private String formatFreq(float frequency) {
        if (frequency == (long) frequency) {
            return String.format("%d", (long) frequency);
        }
        return String.format("%.1f", frequency).replace(",", ".");
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        tag.putString("NetworkName", networkName);
        super.write(tag, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        networkName = tag.getString("NetworkName");
        super.read(tag, clientPacket);
    }
}