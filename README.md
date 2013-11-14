This plugin requires players to enter a command in order to logout safely, or a CombatTag NPC will be left in their place for a duration defined by the server's CombatTag configuration. Once a player issues a logout request through the logout command, a countdown will begin. If no player action events cancel the countdown, the player will be safely disconnected with a configurable message.

The events that cancel a player's logout countdown are configurable. By default, moving, chatting, and interacting with blocks will cancel the countdown.

/logout [/lo, /l] - command to safely log a player out

config.yml:

logout_countdown: integer value in seconds