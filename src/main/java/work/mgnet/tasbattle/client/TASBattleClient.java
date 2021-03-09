package work.mgnet.tasbattle.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.render.SkyProperties;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import org.apache.commons.io.IOUtils;
import work.mgnet.tasbattle.TASBattle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

@Environment(EnvType.CLIENT)
public class TASBattleClient implements ClientModInitializer {

    public static int tickrate = 20;
    public static final byte version = 12;
    public static boolean invalidVersion = false;
    public static boolean didShow = false;

    public static boolean requestRefresh = false;

    public static HashMap<String, String> titles = new HashMap<>();

    public static String readUrl(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return IOUtils.toString(in, "UTF-8");
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
        ClientPlayNetworking.registerGlobalReceiver(TASBattle.atmoschannel, (client, handler, buf, responseSender) -> {
            if (buf.readString().equalsIgnoreCase("nether")) {
                MinecraftClient.getInstance().world.getDimension().natural = false;
                MinecraftClient.getInstance().world.getDimension().bedWorks = false;
                MinecraftClient.getInstance().world.getDimension().respawnAnchorWorks = true;
                MinecraftClient.getInstance().world.getDimension().ultrawarm = true;
                MinecraftClient.getInstance().world.skyProperties = SkyProperties.byDimensionType(DimensionType.THE_NETHER);
            } else {
                MinecraftClient.getInstance().world.getDimension().natural = true;
                MinecraftClient.getInstance().world.getDimension().bedWorks = true;
                MinecraftClient.getInstance().world.getDimension().respawnAnchorWorks = false;
                MinecraftClient.getInstance().world.getDimension().ultrawarm = false;
                MinecraftClient.getInstance().world.skyProperties = SkyProperties.byDimensionType(DimensionType.OVERWORLD);
            }
            requestRefresh = true;
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
