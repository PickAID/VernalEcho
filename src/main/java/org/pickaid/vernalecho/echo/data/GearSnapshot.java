package org.pickaid.vernalecho.echo.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record GearSnapshot(
    ItemStack mainHand,
    ItemStack offHand,
    ItemStack head,
    ItemStack chest,
    ItemStack legs,
    ItemStack feet
) {
    public static final GearSnapshot EMPTY = new GearSnapshot(
        ItemStack.EMPTY,
        ItemStack.EMPTY,
        ItemStack.EMPTY,
        ItemStack.EMPTY,
        ItemStack.EMPTY,
        ItemStack.EMPTY
    );

    public static final Codec<GearSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("mainHand", ItemStack.EMPTY).forGetter(GearSnapshot::mainHand),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("offHand", ItemStack.EMPTY).forGetter(GearSnapshot::offHand),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("head", ItemStack.EMPTY).forGetter(GearSnapshot::head),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("chest", ItemStack.EMPTY).forGetter(GearSnapshot::chest),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("legs", ItemStack.EMPTY).forGetter(GearSnapshot::legs),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("feet", ItemStack.EMPTY).forGetter(GearSnapshot::feet)
    ).apply(instance, GearSnapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GearSnapshot> STREAM_CODEC = StreamCodec.of(
        (buf, snapshot) -> {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.mainHand);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.offHand);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.head);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.chest);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.legs);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot.feet);
        },
        buf -> new GearSnapshot(
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf)
        )
    );

    public static GearSnapshot capture(Player player) {
        return new GearSnapshot(
            player.getItemBySlot(EquipmentSlot.MAINHAND).copy(),
            player.getItemBySlot(EquipmentSlot.OFFHAND).copy(),
            player.getItemBySlot(EquipmentSlot.HEAD).copy(),
            player.getItemBySlot(EquipmentSlot.CHEST).copy(),
            player.getItemBySlot(EquipmentSlot.LEGS).copy(),
            player.getItemBySlot(EquipmentSlot.FEET).copy()
        );
    }
}
