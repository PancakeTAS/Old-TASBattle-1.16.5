package work.mgnet.tasbattle.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mgnet.tasbattle.TASBattle;

@Mixin(PlayerManager.class)
public class MixinRespawn {

    @Inject(method = "respawnPlayer", at = @At("HEAD"))
    public void redorespawnPlayer(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        player.setSpawnPoint(TASBattle.isRunning ? TASBattle.worldReg : player.getServer().getOverworld().getRegistryKey(), new BlockPos(0, 100, 0), player.getSpawnAngle(), true, false);
    }

}
