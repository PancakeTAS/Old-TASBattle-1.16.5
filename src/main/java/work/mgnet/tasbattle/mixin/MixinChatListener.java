package work.mgnet.tasbattle.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.client.TASBattleClient;

@Mixin(ChatHud.class)
public class MixinChatListener {

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;)V")
    public void injectaddMessage(Text text, CallbackInfo ci) {
        if (text.getString().startsWith("§9[§6MG§9Network] §7Updating Tickrate to §b")) {
            int tickrate = Integer.parseInt(text.getString().substring(43));
            TASBattleClient.tickrate = tickrate;
            MinecraftClient.getInstance().renderTickCounter.tickTime = 1000 / tickrate;
        }
    }

}
