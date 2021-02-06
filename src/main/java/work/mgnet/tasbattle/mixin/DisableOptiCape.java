package work.mgnet.tasbattle.mixin;

import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public class DisableOptiCape {

    @Inject(at = @At("HEAD"), cancellable = true, method = "render")
    public void cancelRender(CallbackInfo ci) {
        ci.cancel();
    }

}
