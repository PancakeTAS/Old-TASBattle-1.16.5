package work.mgnet.tasbattle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinHackMultiplayerScreen {

    @Inject(method = "openScreen", cancellable = true, at = @At("HEAD"))
    public void redoopenScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (screen instanceof MultiplayerScreen) {
            ((MinecraftClient) (Object) this).openScreen(new TitleScreen());
            ci.cancel();
        }
    }

}
