package work.mgnet.tasbattle.mixin;

import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class MixinToast {

    @Inject(method = "add", cancellable = true, at = @At("HEAD"))
    public void redoadd(CallbackInfo ci) {
        ci.cancel();
    }

}
