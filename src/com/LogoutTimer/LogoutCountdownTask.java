package com.LogoutTimer;

import org.bukkit.scheduler.BukkitRunnable;

public class LogoutCountdownTask extends BukkitRunnable {

	private final LogoutTimer plugin;
	
	public LogoutCountdownTask(LogoutTimer plugin) {
		this.plugin = plugin;
	}
	
	public void run() {
		this.plugin.checkAndDecrementCounters();
	}

}
