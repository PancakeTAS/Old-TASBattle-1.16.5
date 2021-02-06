package work.mgnet.tasbattle.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import work.mgnet.tasbattle.TASBattle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class TASBattleClient implements ClientModInitializer {

    public static int tickrate = 20;
    public static final byte version = 8;
    public static boolean invalidVersion = false;
    public static boolean didShow = false;

    public static HashMap<String, String> titles = new HashMap<>();

    public static String readUrl(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return IOUtils.toString(in);
        }
    }

    @Override
    public void onInitializeClient() {
        try {
            for (String s : readUrl(new URL("http://mgnet.work/tags.txt")).split(";")) {
                titles.put(s.split(":")[0], s.split(":")[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ClientPlayNetworking.registerGlobalReceiver(TASBattle.tickrateChannel, (client, handler, buf, responseSender) -> {
            tickrate = buf.readInt();
            client.renderTickCounter.tickTime = 1000 / tickrate;
        });
        ClientPlayNetworking.registerGlobalReceiver(new Identifier("updater"), (client, handler, buf, responseSender) -> {
            if (version != buf.readByte()) {
                invalidVersion = true;
                MinecraftClient.getInstance().openScreen(new FatalErrorScreen(new LiteralText("You are using an older version of this mod"), new LiteralText("Please restart the Game. The Updater will reinstall the mod")) {
                    protected void init() {

                    }
                });
            }
        });
    }

}
