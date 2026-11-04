package ru.d2.elysium.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.d2.elysium.ElysiumClient;

@Mixin(ClientPacketListener.class)
public abstract class StatsPacketMixin {
    // TAIL executes only on the client thread, after vanilla dispatch and stat updates.
    @Inject(method = "handleAwardStats", at = @At("TAIL"))
    private void elysium$statistics(ClientboundAwardStatsPacket packet, CallbackInfo ci) {
        ElysiumClient.instance().receive((ClientPacketListener) (Object) this, packet);
    }
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void elysium$damage(net.minecraft.network.protocol.game.ClientboundDamageEventPacket packet, CallbackInfo ci) {
        ElysiumClient.instance().damage((ClientPacketListener) (Object) this, packet);
    }
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void elysium$death(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet, CallbackInfo ci) {
        ElysiumClient.instance().death((ClientPacketListener) (Object) this, packet);
    }
}
