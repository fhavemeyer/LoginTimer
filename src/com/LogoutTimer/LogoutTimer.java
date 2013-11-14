package com.LogoutTimer;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.*;
import org.bukkit.event.*;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.command.*;
import org.bukkit.util.Vector;
import org.bukkit.ChatColor;

import com.trc202.CombatTagApi.CombatTagApi;
import com.trc202.CombatTag.CombatTag;


public class LogoutTimer extends JavaPlugin implements Listener {
	
	private CombatTagApi combatApi;	
	
	private Integer scheduledTaskID;
	
	private int countdown;
	private String disconnectMessage;
	private String cancelMessage = "Canceling logout!";
	
	private boolean cancelOnMove;
	private boolean cancelOnChat;
	private boolean cancelOnInteract;
	
	private HashMap<String, Integer> logoutCountdown;
	private HashMap<String, Boolean> permissionToLog;
	
	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);
		
		this.logoutCountdown = new HashMap<>();
		this.permissionToLog = new HashMap<>();
		
		this.countdown = getConfig().getInt("logout_countdown");
		this.disconnectMessage = getConfig().getString("disconnect_message");
		
		this.cancelOnMove = getConfig().getBoolean("cancel_on.move");
		this.cancelOnChat = getConfig().getBoolean("cancel_on.chat");
		this.cancelOnInteract = getConfig().getBoolean("cancel_on.interact");
		
		if (getServer().getPluginManager().getPlugin("CombatTag") != null) {
			combatApi = new CombatTagApi((CombatTag)getServer().getPluginManager().getPlugin("CombatTag"));
		}
		
		if (combatApi == null) {
			getLogger().severe("CombatTag not loaded! Disabling LogoutTimer!");
			this.setEnabled(false);
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
		if (this.cancelOnMove) {
			// Vector.equals(vec) has been giving me problems when rotating the mouse, so let's cast these to ints
			Vector from = new Vector((int)e.getFrom().getX(), (int)e.getFrom().getY(), (int)e.getFrom().getZ());
			Vector to = new Vector((int)e.getTo().getX(), (int)e.getTo().getY(), (int)e.getTo().getZ());
			
			if (!from.equals(to)) {
				this.checkAndCancelLogout(e.getPlayer(), "You have moved!");
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent e) {
		if (this.cancelOnInteract) {
			this.checkAndCancelLogout(e.getPlayer(), "You have interacted with a block!");
		}
	}
	
	@EventHandler
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent e) {
		if (this.cancelOnChat) {
			this.checkAndCancelLogout(e.getPlayer(), "You have used chat!");
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
			} else if (combatApi.isInCombat(logger)) {
				logger.sendMessage(ChatColor.RED + "You cannot log out while in combat!");
				this.stopCountdownForPlayer(playerName);
				this.removeLogoutPermissionForPlayer(playerName);
			} else {
				if (timeLeft == 0) {
					this.stopCountdownForPlayer(playerName);
					this.giveLogoutPermissionToPlayer(playerName);
					logger.kickPlayer(this.disconnectMessage);
				} else {
					logger.sendMessage(ChatColor.RED + Integer.toString(timeLeft) + " seconds until you are safely logged out!");
					timeLeft -= 1;
					entry.setValue(timeLeft);
				}
			}
		}
	}
	
	public void checkAndCancelLogout(Player p, String customMessage) {
		String playerName = p.getDisplayName();
		customMessage = (customMessage != null ? customMessage + " " : "");
		
		if (this.playerHasPermissionToLogout(playerName)) {
			this.removeLogoutPermissionForPlayer(playerName);
		} else if (this.logoutCountdown.containsKey(playerName)) {
			this.stopCountdownForPlayer(playerName);
			p.sendMessage(ChatColor.RED + customMessage + this.cancelMessage);
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
}
