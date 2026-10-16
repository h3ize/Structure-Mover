package com.example.structuremover;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: the outer shell of the current selection (block positions), or an empty array to clear it. */
public record SelectionSyncPayload(long[] shell) implements CustomPacketPayload {
    public static final Type<SelectionSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(StructureMover.MODID, "selection_sync"));

    public static final StreamCodec<FriendlyByteBuf, SelectionSyncPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.shell.length);
                for (long l : p.shell) buf.writeLong(l);
            },
            buf -> {
                int n = buf.readVarInt();
                long[] a = new long[n];
                for (int i = 0; i < n; i++) a[i] = buf.readLong();
                return new SelectionSyncPayload(a);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
