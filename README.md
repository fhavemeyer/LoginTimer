This plugin requires players to enter a command before logging out, or a CombatTag NPC will be left in their place for a duration defined by the server's CombatTag configuration.

If the player moves while the countdown is active, it will be canceled. Likewise, if the player moves once the countdown has completed but before disconnecting, the player will have to begin the countdown anew. Countodwn duration is configurable in the plugin's config.yml

/logout [/lo] - command to safely log a player out

config.yml:

logout_countdown: integer value in seconds