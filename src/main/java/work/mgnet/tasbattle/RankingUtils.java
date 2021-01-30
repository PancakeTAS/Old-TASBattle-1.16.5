package work.mgnet.tasbattle;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class RankingUtils {
	
	public static void onDeath(ServerPlayerEntity p) {
		p.sendMessage(new LiteralText("§b� §e[-10 Points] You died"), true);
		TASBattle.stats.updatePoints(p, -10);
	}
	
	public static int getRank(ServerPlayerEntity pl) {
		StatsUtils.Stats s = TASBattle.stats.getStats(pl);
		int p = s.points;
		if (p < 500) return 0;
		else if (p < 2000) return 1;
		else if (p < 5000) return 2;
		else if (p < 10000) return 3;
		else if (p < 50000) return 4;
		else return 5;
	}
	
 	public static void onKill(ServerPlayerEntity killer, ServerPlayerEntity p) {
		int killerRank = getRank(killer);
		int myRank = getRank(p);
		
		int points = 0;
		
		if (killerRank == myRank) points = 1;
		else if ((killerRank + 1) == myRank) points = 1;
		else if ((killerRank + 2) == myRank) points = 25;
		else if ((killerRank + 3) == myRank) points = 250;
		else if ((killerRank + 4) == myRank) points = 2500;
		else if ((killerRank + 5) == myRank) points = 2500;
		
		killer.sendMessage(new LiteralText("§b� §e[+" + points + " Points] You killed a player"), true);
		p.sendMessage(new LiteralText("§b� §e[-" + points + " Points] You were killed by a player"), true);

		TASBattle.stats.updatePoints(killer, points);
		TASBattle.stats.updatePoints(p, -points);
		
	}
	
	public static void onWin(ServerPlayerEntity winner) {
		winner.sendMessage(new LiteralText("§b� §e[+25 Points] You won a Game"), true);
		TASBattle.stats.updatePoints(winner, 25);
	}
	
}
