package no.runsafe.combatcooldown;

import no.runsafe.framework.api.IScheduler;
import no.runsafe.framework.api.IWorld;
import no.runsafe.framework.api.event.plugin.IPluginDisabled;
import no.runsafe.framework.api.player.IPlayer;

import java.util.concurrent.ConcurrentHashMap;

public class CombatMonitor implements IPluginDisabled
{
	public CombatMonitor(IScheduler scheduler, Config config)
	{
		this.scheduler = scheduler;
		this.config = config;
	}

	public void leaveCombat(IPlayer player)
	{
		if (!this.combatTimers.containsKey(player))
			return;

		if (!player.isPvPFlagged() && !player.isDead() && !player.isSpectator() && !player.isCreative() && !player.isVanished())
		{
			// Don't let a player leave combat if they hid in a non-pvp region
			playerHidingOutsidePVPRegion(player);
			return;
		}
		this.warningTimers.remove(player);
		this.combatTimers.remove(player);
		player.sendColouredMessage(config.getLeavingCombatMessage());
	}

	public boolean isInCombat(IPlayer player)
	{
		return this.combatTimers.containsKey(player);
	}

	@Override
	public void OnPluginDisabled()
	{
		this.combatTimers.clear();
	}

	private boolean monitoringWorld(IWorld world)
	{
		if (world == null)
			return false;

		return config.getPvpWorlds().contains(world.getName());
	}

	public void engageInDergonCombat(IPlayer player)
	{
		if (config.shouldIncludeDergons() && monitoringWorld(player.getWorld()) && player.isPvPFlagged())
			engagePlayer(player);
	}

	public void engageInCombat(IPlayer firstPlayer, IPlayer secondPlayer)
	{
		if (!this.monitoringWorld(firstPlayer.getWorld())
			|| !this.monitoringWorld(secondPlayer.getWorld())
			|| !firstPlayer.isPvPFlagged()
			|| !secondPlayer.isPvPFlagged()
		)
			return;

		this.engagePlayer(firstPlayer);
		this.engagePlayer(secondPlayer);
	}

	private void engagePlayer(IPlayer player)
	{
		if (!isInCombat(player))
			player.sendColouredMessage(config.getEnteringCombatMessage());

		this.registerPlayerTimer(player);
	}

	private void registerPlayerTimer(final IPlayer player)
	{
		if (this.warningTimers.containsKey(player))
		{
			this.scheduler.cancelTask(this.warningTimers.get(player));
			warningTimers.remove(player);
		}

		if (this.combatTimers.containsKey(player))
			this.scheduler.cancelTask(this.combatTimers.get(player));

		this.combatTimers.put(player, this.scheduler.startSyncTask(() -> leaveCombat(player), config.getCombatTime()));
	}

	private void playerHidingOutsidePVPRegion(IPlayer player)
	{
		player.sendColouredMessage(config.getWarningProtectedRegion(), config.getWarningTime());
		if (this.warningTimers.containsKey(player))
			this.scheduler.cancelTask(this.combatTimers.get(player));

		this.warningTimers.put(player, this.scheduler.startSyncTask(() ->
		{
			// Last minute gamemode check
			if (player.isSpectator() || player.isCreative() || player.isVanished())
			{
				leaveCombat(player);
				return;
			}

			if (player.isPvPFlagged())
			{
				engagePlayer(player);
				return;
			}
			player.setHealth(0); // Player is still hiding in non-pvp region, kill them.
		}, config.getCombatTime()));
	}

	private final ConcurrentHashMap<IPlayer, Integer> combatTimers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<IPlayer, Integer> warningTimers = new ConcurrentHashMap<>();
	private final IScheduler scheduler;
	private final Config config;
}
