package work.mgnet.tasbattle.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import work.mgnet.tasbattle.TASBattle;

@Environment(EnvType.CLIENT)
public class TASBattleClient implements ClientModInitializer {

    public static int tickrate = 20;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(TASBattle.tickrateChannel, (client, handler, buf, responseSender) -> {
            tickrate = buf.readInt();
            client.renderTickCounter.tickTime = 1000 / tickrate;
        });
    }

}
