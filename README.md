# SimpleTeams

## SimpleTeams is a simple server-side mod that allows players to join or create a team. Players in the same team cannot directly damage each other! Perfect for co-op playthroughs, especially on heavily modded modpacks!

### Damage sources that WILL NOT damage the player when they come from a team member include but not limited to:
-   Direct hits, sword or other.
-   Arrows fired by a player.
-   TNT ignited by a player (with flint and steel or arrow).
-   Indirect sweeping attacks by a player.
-   Any other attack where the True Damage source is a team member.

### Damage sources that WILL damage the player even when they come from a team member include but not limited to:
-   Falling anvils
-   Fall damage caused by breaking blocks underneath another player
-   TNT ignited by redstone
-   Arrows fired from a dispenser activated by a player.
-   Any other way you might want to prank your friend.



## Commands:

- **/team create [TeamName]** <br />
    Creates a team with the specified name. Newly created teams are private by default. Newly created teams have friendly fire turned off by default. The player that created the team is the team owner by default. Team names are case sensitive.

- **/team join [TeamName]** <br />
    If the specified team is public, using this command will join the specified team if the player is not already in a team. If a team is NOT public, the player needs an invite first.

- **/team leave** <br />
    Leaves your current team. If you are the owner of the team and your team is not empty, you cannot leave the team until all members have been kicked or have left, or until you have tranfered ownership of the team to another team member.

- **/team invite [PlayerName]** <br />
    Invite the specified player to your team. <br />
    This command can only be used by the team owner.

- **/team kick [TeamMember]** <br />
    Kicks the specified player from the team. <br />
    This command can only be used by the team owner.

- **/team ban [PlayerName]** <br />
    Bans a player from the team, if the player is currently in the team it kicks them as well. Banned players cannot join the team if the team is public, and cannot be invited to the team. <br />
    This command can only be used by the team owner.

- **/team unban [PlayerName]** <br />
    Unbans a player from the team. <br />
    This command can only be used by the team owner.
  
- **/team invites** <br />
    Shows all team invites you have received.

- **/team toggle [public | hurt]** <br />
    Toggles if the team is public or private, or toggles if friendly fire is enabled or disabled. Any player can join public teams unless they are banned from said team. **If friendly fire is enabled, players in that team can hurt eachother like normal.** <br />
    This command can only be used by the team owner.

- **/team changeowner [TeamMember]** <br />
    Transfers ownership of the team to the specified team member. <br />
    This command can only be used by the team owner.

- **/team info** <br />
    Shows information about your current team, including the owner, the members, and the status of the publicity and friendly fire.

- **/team list** <br />
    Shows a list of all public teams on the server.
