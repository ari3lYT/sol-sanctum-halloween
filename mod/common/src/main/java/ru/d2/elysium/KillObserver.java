package ru.d2.elysium;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import ru.d2.elysium.core.Counter;
import ru.d2.elysium.core.Attribution;

/** Only server damage/death packets enter the tracker; attacking or breaking locally earns nothing. */
public final class KillObserver {
    private final Attribution attribution = new Attribution();

    public void clear() { attribution.clear(); }
    private static int pumpkin(Mob mob) {
        var head = mob.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(Items.JACK_O_LANTERN)) return 2;
        if (head.is(Items.CARVED_PUMPKIN) || head.is(Items.PUMPKIN)) return 1;
        return 0;
    }
    public void damage(Minecraft client, ClientboundDamageEventPacket packet) {
        if (client.player == null || client.level == null) return;
        if (!(client.level.getEntity(packet.entityId()) instanceof Mob mob)) return;
        attribution.damage(mob.getUUID().toString(), BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
                pumpkin(mob), packet.sourceCauseId(), client.player.getId(), client.player.tickCount);
    }
    public boolean death(Minecraft client, ClientboundEntityEventPacket packet, Counter counter) {
        if (packet.getEventId() != 3 || client.player == null || client.level == null) return false;
        if (!(packet.getEntity(client.level) instanceof Mob mob)) return false;
        var candidate = attribution.death(mob.getUUID().toString(), BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
                pumpkin(mob), client.player.tickCount, System.currentTimeMillis());
        return candidate != null && counter.observe(candidate.uuid(), candidate.entityType(), candidate.glowing(), candidate.observedAt());
    }
    public void expire(Minecraft client) {
        if (client.player != null) attribution.expire(client.player.tickCount);
    }
}
