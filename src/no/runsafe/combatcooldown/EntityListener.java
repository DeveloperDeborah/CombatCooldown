package no.runsafe.combatcooldown;

import no.runsafe.framework.api.event.entity.IEntityDamageByEntityEvent;
import no.runsafe.framework.api.event.player.IPlayerCustomEvent;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.entity.ProjectileEntity;
import no.runsafe.framework.minecraft.entity.RunsafeEntity;
import no.runsafe.framework.minecraft.entity.RunsafeProjectile;
import no.runsafe.framework.minecraft.event.entity.RunsafeEntityDamageByEntityEvent;
import no.runsafe.framework.minecraft.event.player.RunsafeCustomEvent;

public class EntityListener implements IEntityDamageByEntityEvent, IPlayerCustomEvent
{
	public EntityListener(CombatMonitor combatMonitor)
	{
		this.combatMonitor = combatMonitor;
	}

	@Override
	public void OnPlayerCustomEvent(RunsafeCustomEvent event)
	{
		if (!event.getEvent().equals("runsafe.dergon.mount"))
			return;

		IPlayer player = event.getPlayer();
		if (player.isVanished())
			return;

		combatMonitor.engageInDergonCombat(player);
		Plugin.Debugger.debugFine(String.format(
			"Player %s is being picked up by a Dergon - Blocking Commands if able.",
			player.getName()
		));
	}

	@Override
	public void OnEntityDamageByEntity(RunsafeEntityDamageByEntityEvent event)
	{
		if (!(event.getEntity() instanceof IPlayer))
			return;

		IPlayer victim = (IPlayer) event.getEntity();
		if (victim.isVanished())
			return;

		IPlayer attackingPlayer = null;
		RunsafeEntity attacker = event.getDamageActor();

		Plugin.Debugger.debugFine(String.format(
			"Player %s is being attacked by a %s.",
			victim.getName(),
			attacker.getEntityType().getName()
		));

		if (attacker instanceof IPlayer)
			attackingPlayer = (IPlayer) attacker;
		else if (attacker instanceof RunsafeProjectile)
		{
			RunsafeProjectile projectile = (RunsafeProjectile) attacker;
			if (!(projectile.getEntityType() == ProjectileEntity.Egg || projectile.getEntityType() == ProjectileEntity.Snowball))
				attackingPlayer = projectile.getShootingPlayer();
		}

		if (attackingPlayer == null)
		{
			Plugin.Debugger.debugFine("Victim is not being attacked by a player.");
			return;
		}

		if (attackingPlayer.isVanished() || attackingPlayer.shouldNotSee(victim) || victim.equals(attackingPlayer))
		{
			Plugin.Debugger.debugFine("Victim being attacked by exempted player.");
			return;
		}

		this.combatMonitor.engageInCombat(attackingPlayer, victim);
		Plugin.Debugger.debugFine(String.format(
			"Player %s engaged in PvP with %s - Blocking commands",
			attackingPlayer.getName(),
			victim.getName()
		));
	}

	private final CombatMonitor combatMonitor;
}
