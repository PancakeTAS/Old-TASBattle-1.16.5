package work.mgnet.tasbattle.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.MessageType;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import work.mgnet.tasbattle.TASBattle;

import java.io.IOException;
import java.util.UUID;

@Mixin(PlayerManager.class)
public abstract class MixinPlayerManager {

    @Shadow @Final private MinecraftServer server;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    public void redoonPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        TASBattle.playerJoin(player);
    }

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    public void redobroadcastChatMessage(Text message, MessageType type, UUID senderUuid, CallbackInfo ci) {
        try {String playername = server.getPlayerManager().getPlayer(senderUuid).getName().getString();
        if (message.getString().contains("left the")) {
            message = new LiteralText("§b» §a" + message.getString().split(" left")[0].trim() + " §7left the game");
            server.sendSystemMessage(message, senderUuid);
            sendToAll(new GameMessageS2CPacket(message, type, senderUuid));
            ci.cancel();
        } else if (message.getString().contains("joined the")) {
            message = new LiteralText("§b» §a" + message.getString().split(" joined")[0].trim() + " §7joined the game");
            server.sendSystemMessage(message, senderUuid);
            sendToAll(new GameMessageS2CPacket(message, type, senderUuid));
            ci.cancel();
        } else if (message.getString().contains("<" + playername + ">")) {
            message = new LiteralText(message.getString().replaceFirst("<" + playername + ">", "§b" + playername + " §a»§7"));
            server.sendSystemMessage(message, senderUuid);
            sendToAll(new GameMessageS2CPacket(message, type, senderUuid));
            ci.cancel();
        }} catch (Exception e) {

        }
        //String playerrank = "";
        //message = new LiteralText(message.getString().replaceFirst("<" + playername + ">", "§7<§b" + playerrank + " §a" + playername + "»§7"));
    }

    @Shadow
    public abstract void sendToAll(Packet<?> packet);

    @Inject(method = "remove", at = @At("TAIL"))
    public void redoremove(ServerPlayerEntity player, CallbackInfo ci) {
        if (TASBattle.players.contains(player)) {
            try {
                TASBattle.playerDeath(player, 0, 0, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
