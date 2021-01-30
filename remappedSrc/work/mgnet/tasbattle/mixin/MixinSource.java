package work.mgnet.tasbattle.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import work.mgnet.tasbattle.client.TASBattleClient;

@Mixin(SoundSystem.class)
public class MixinSource {

    @Inject(method = "getAdjustedPitch", at = @At("HEAD"), cancellable = true)
    private void redogetAdjustedPitch(SoundInstance soundInstance, CallbackInfoReturnable<Float> ci) {
        ci.setReturnValue((TASBattleClient.tickrate / 20F) * MathHelper.clamp(soundInstance.getPitch(), 0.5F, 2.0F));
        ci.cancel();
    }

}
