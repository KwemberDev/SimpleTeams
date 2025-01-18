# SimpleTeams

## SimpleTeams is a simple server-side mod that allows players to join or create a team. Players in the same team cannot directly damage eachother! Perfect for coop playthroughs, especially on heavily modded modpacks!

### Damage sources that will not damage the player when they come from a team member include but not limited to:
-   Direct hits, sword or other.
-   Arrows fired by a player.
-   TNT ignited by a player (with flint and steel or arrow).
-   Indirect sweeping attacks by a player.

### Damage sources that WILL dmage the player even when they come from a team member include but not limited to:
-   Falling anvils
-   Fall damage caused by breaking blocks underneath another player
-   TNT ignited by redstone
-   Arrows fired from a dispenser activated by a player.
-   Any other way you might want to prank your friend.


### Commands:

- /team create <TeamName>
    Creates a team with the specified name. Newly created teams are private by default. Newly created teams have friendly fire turned off by default. The player that created the team is owner by default.

- /team join <TeamName> 
    If the specified team is public, using this command will join the specified team if the player is not already in a team. If a team is NOT public, the player needs an invite first.

- /team leave
    Leaves your current team. If you are the owner of the team and your team is not empty, you cannot leave the team until all members have been kicked or have left, or until you have tranfered ownership of the team to another team member.

- /team invite <PlayerName>
    Invite the specified player to your team.
    This command can only be used by the team owner.

- /team kick <TeamMember>
    Kicks the specified player from the team.
    This command can only be used by the team owner.

- /team invites
    Shows all team invites you have received.

- /team toggle [public | hurt]
    Toggles if the team is public or private, or toggles if friendly fire is enabled or disabled.
    This command can only be used by the team owner.

- /team changeowner <TeamMember>
    Transfers ownership of the team to the specified team member. 
    This command can only be used by the team owner.

- /team info
    Shows information about your current team, including the owner, the members, and the status of the publicity and the friendly fire.

- /team list
    Shows a list of all public teams on the server.
