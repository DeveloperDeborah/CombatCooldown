package no.runsafe.combatcooldown;

import no.runsafe.framework.RunsafeConfigurablePlugin;
import no.runsafe.framework.api.log.IDebug;
import no.runsafe.framework.features.Events;

public class Plugin extends RunsafeConfigurablePlugin
{
	public static IDebug Debugger;

	@Override
	protected void pluginSetup()
	{
		Debugger = getComponent(IDebug.class);

		this.addComponent(Config.class);
		this.addComponent(Events.class);
		this.addComponent(CombatMonitor.class);
		this.addComponent(EntityListener.class);
		this.addComponent(PlayerListener.class);
	}
}
