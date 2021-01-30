package work.mgnet.tasbattle.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.util.PlayerHandler;

@Mixin(ClientWorld.class)
public class MixinClientWorld {

    @Inject(method = "addPlayer", at = @At("RETURN"))
    private void addPlayer(int id, AbstractClientPlayerEntity player, CallbackInfo info) {
        PlayerHandler.onPlayerJoin(player);
    }

}
