package work.mgnet.tasbattle;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.SpreadPlayersCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;

import java.util.List;

public class SpreadplayersTheft {

    public static void spread(ServerCommandSource source, List<ServerPlayerEntity> collection, ServerWorld world) throws CommandSyntaxException {
        source = source.withWorld(world);
        Vec2f center = new Vec2f(0.0f, 0.0f);
        float maxRange = 250.0f;
        float spreadDistance = 20.0f;
        SpreadPlayersCommand.execute(source, center, spreadDistance, maxRange, 255, false, collection);
    }

}
