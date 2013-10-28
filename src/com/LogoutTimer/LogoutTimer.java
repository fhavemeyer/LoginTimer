package com.LogoutTimer;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.*;
import org.bukkit.event.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.command.*;
import org.bukkit.util.Vector;
import org.bukkit.ChatColor;

import com.trc202.CombatTagApi.CombatTagApi;
import com.trc202.CombatTag.CombatTag;


public class LogoutTimer extends JavaPlugin implements Listener {
	
	private CombatTagApi combatApi;	
	
	private Integer scheduledTaskID;
	
	private int countdown;
	
	private HashMap<String, Integer> logoutCountdown;
	private HashMap<String, Boolean> permissionToLog;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		this.logoutCountdown = new HashMap<>();
		this.permissionToLog = new HashMap<>();
		
		this.countdown = getConfig().getInt("logout_countdown");
		
		if (getServer().getPluginManager().getPlugin("CombatTag") != null) {
			combatApi = new CombatTagApi((CombatTag)getServer().getPluginManager().getPlugin("CombatTag"));
		}
		
		this.scheduledTaskID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new LogoutCountdownTask(this), 20, 20);
		
		getLogger().info("LogoutTimer initialized.");
	}
	
	@Override
	public void onDisable() {
		this.logoutCountdown = null;
		this.permissionToLog = null;
		getServer().getScheduler().cancelTask(this.scheduledTaskID);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		final Player logged = e.getPlayer();
		String playerName = logged.getPlayerListName();
		getLogger().info("Player quit: " + playerName);
		
		if (!combatApi.isInCombat(logged)) {
			if (this.playerHasPermissionToLogout(playerName)) {
				getLogger().info(playerName + " safely logged out.");
			} else {
				combatApi.tagPlayer(logged);
				getLogger().info(playerName + " did not safely log out and left his NPC.");
			}
			
			this.removeLogoutPermissionForPlayer(playerName);
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		final Player moved = e.getPlayer();
		String playerName = moved.getPlayerListName();
		
		// Vector.equals(vec) has been giving me problems when rotating the mouse, so let's cast these to ints
		Vector from = new Vector((int)e.getFrom().getX(), (int)e.getFrom().getY(), (int)e.getFrom().getZ());
		Vector to = new Vector((int)e.getTo().getX(), (int)e.getTo().getY(), (int)e.getTo().getZ());
		
		if (!from.equals(to)) {
			if (this.playerHasPermissionToLogout(playerName)) {
				this.removeLogoutPermissionForPlayer(playerName);
				moved.sendMessage(ChatColor.RED + "You have moved! You must restart your logout request!");
			} else if (this.logoutCountdown.containsKey(playerName)) {
				this.stopCountdownForPlayer(playerName);
				moved.sendMessage(ChatColor.RED + "You have moved! Canceling logout request!");
			}
		}
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			Player p = (Player)sender;
			String playerName = p.getPlayerListName();
			
			if (combatApi.isInCombat(p)) {
				p.sendMessage(ChatColor.RED + "You are currently in combat and can't log out!");
			} else if (logoutCountdown.get(playerName) != null) {
				p.sendMessage(ChatColor.RED + "You have already initiated logout!");
			} else {
				this.logoutCountdown.put(p.getPlayerListName(), this.countdown);
			}
			return true;
		} else {
			sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
		}
		return false;
	}

	public void checkAndDecrementCounters() {
		for (Map.Entry<String, Integer> entry : this.logoutCountdown.entrySet()) {
			String playerName = entry.getKey();
			int timeLeft = entry.getValue();
			
			Player logger = getServer().getPlayer(playerName);
			
			if (logger == null) {
				// Player is already tagged
				this.stopCountdownForPlayer(playerName);
			} else {
				timeLeft -= 1;
				if (timeLeft == 0) {
					this.stopCountdownForPlayer(playerName);
					this.giveLogoutPermissionToPlayer(playerName);
					logger.sendMessage(ChatColor.GREEN + "You may now safely log out!");
				} else {
					entry.setValue(timeLeft);
					logger.sendMessage(ChatColor.RED + Integer.toString(timeLeft) + " seconds remaining!");
				}
			}
		}
	}
	
	public void stopCountdownForPlayer(String playerName) {
		if (this.logoutCountdown.containsKey(playerName)) {
			this.logoutCountdown.remove(playerName);
		}
	}
	
	public void giveLogoutPermissionToPlayer(String playerName) {
		this.permissionToLog.put(playerName, true);
	}
	
	public void removeLogoutPermissionForPlayer(String playerName) {
		if (this.permissionToLog.containsKey(playerName)) {
			this.permissionToLog.remove(playerName);
		}
	}
	
	public boolean playerHasPermissionToLogout(String playerName) {
		return this.permissionToLog.containsKey(playerName);
	}
	
	public void profileMaps() {
		getLogger().info("Players waiting to log: [" + this.logoutCountdown.size() + "]");
		getLogger().info("Players with permission to log: [" + this.permissionToLog.size() + "]");
	}
}
