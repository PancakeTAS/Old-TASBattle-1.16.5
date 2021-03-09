package work.mgnet.tasbattle.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.TASBattle;
import work.mgnet.tasbattle.client.TASBattleClient;

import javax.security.auth.callback.Callback;
import java.io.*;
import java.net.URL;

@Mixin(MinecraftClient.class)
public class MixinHackMultiplayerScreen {

    @Inject(method = "openScreen", cancellable = true, at = @At("HEAD"))
    public void redoopenScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (screen instanceof MultiplayerScreen) {
            ((MinecraftClient) (Object) this).openScreen(new TitleScreen());
            ci.cancel();
        } else if (screen == null && TASBattleClient.invalidVersion) {
            ci.cancel();
        } else if (TASBattleClient.invalidVersion && !(screen instanceof FatalErrorScreen)) {
            ci.cancel();
        } else if (screen == null && !TASBattleClient.didShow) {
            TASBattleClient.didShow = true;
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeByte(TASBattleClient.version);
            ClientPlayNetworking.send(new Identifier("version"), buf);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void injectrender(CallbackInfo ci) {
        if (TASBattleClient.requestRefresh) {
            TASBattleClient.requestRefresh = false;
            MinecraftClient.getInstance().worldRenderer.reload();
        }
    }

    @Inject(method = "close", at = @At("RETURN"), cancellable = true)
    public void redoclose(CallbackInfo ci) {
        File mod = new File("mods/tasbattle.jar");
        try {
            URL url = new URL("http://mgnet.work/tasbattle.jar");
            InputStream stream = url.openStream();
            OutputStream s2 = new FileOutputStream(mod);
            PrintWriter writer = new PrintWriter(mod);
            writer.print("");
            writer.close();

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                s2.write(buffer, 0, bytesRead);
            }

            stream.close();
            s2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
