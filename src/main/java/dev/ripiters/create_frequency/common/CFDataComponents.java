package dev.ripiters.create_frequency.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CFDataComponents {
    private static final String BINDS_KEY = "controller_binds";

    public static List<Float> getBinds(ItemStack stack) {
        List<Float> list = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BINDS_KEY)) return list;
        ListTag listTag = tag.getList(BINDS_KEY, 5); // 5 = FloatTag type
        for (int i = 0; i < listTag.size(); i++) {
            list.add(listTag.getFloat(i));
        }
        return list;
    }

    public static void setBinds(ItemStack stack, List<Float> binds) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag listTag = new ListTag();
        for (Float f : binds) {
            listTag.add(FloatTag.valueOf(f));
        }
        tag.put(BINDS_KEY, listTag);
    }
}