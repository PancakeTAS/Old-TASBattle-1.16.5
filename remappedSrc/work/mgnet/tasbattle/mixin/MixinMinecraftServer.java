package work.mgnet.tasbattle.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.TASBattle;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow public abstract PlayerManager getPlayerManager();

    @Shadow @Nullable public abstract ServerWorld getWorld(RegistryKey<World> key);

    @ModifyConstant(method = "runServer", constant = @Constant(longValue = 50L))
    private long serverTickWaitTime(long ignored) {
        return 1000 / TASBattle.tickrate;
    }

    @Inject(method = "tick", cancellable = true, at = @At("HEAD"))
    public void redotick(CallbackInfo ci) {
        if (TASBattle.rl) {
            TASBattle.rl = false;
            TASBattle.reloadMap((MinecraftServer) (Object) this);
        }
    }

}
