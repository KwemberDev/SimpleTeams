package simpleteams;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import simpleteams.handler.DamageHandler;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Mod(modid = SimpleTeams.MODID, version = SimpleTeams.VERSION, name = SimpleTeams.NAME, serverSideOnly = true, acceptableRemoteVersions = "*")
public class SimpleTeams {
    public static final String MODID = "simpleteams";
    public static final String VERSION = "0.0.3";
    public static final String NAME = "SimpleTeams";
    public static final Logger LOGGER = LogManager.getLogger();

    @Instance(MODID)
    public static SimpleTeams instance;

    private static final File TEAMS_FILE = new File("teams.dat");
    public static final Map<String, Team> teams = new HashMap<>();
    public static final Map<UUID, String> playerTeams = new HashMap<>();
    private static final Map<UUID, List<String>> pendingInvites = new HashMap<>();

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        loadTeams();
        event.registerServerCommand(new TeamCommand());
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new DamageHandler());  // Register DamageHandler here
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntity();
            pendingInvites.putIfAbsent(player.getUniqueID(), new ArrayList<>());
        }
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        saveTeams();
    }

    private static void saveTeams() {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(TEAMS_FILE.toPath()))) {
            out.writeObject(teams);
            out.writeObject(playerTeams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadTeams() {
        if (!TEAMS_FILE.exists()) return;

        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(TEAMS_FILE.toPath()))) {
            Map<String, Team> loadedTeams = (Map<String, Team>) in.readObject();
            Map<UUID, String> loadedPlayerTeams = (Map<UUID, String>) in.readObject();
            teams.putAll(loadedTeams);
            playerTeams.putAll(loadedPlayerTeams);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static class TeamCommand extends CommandBase {

        @Override
        public String getName() {
            return "team";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/team <create|invite|invites|toggle|join|leave|changeowner|info|kick|ban|unban>";
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0; // allows non operator players to use the command.
        }

        @Override
        public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
            return true;
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (!(sender instanceof EntityPlayer)) {
                sender.sendMessage(new TextComponentString("This command can only be used by players."));
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            UUID playerUUID = player.getUniqueID();

            if (args.length < 1) {
                sender.sendMessage(new TextComponentString(TextFormatting.RED + "/team <create|invite|invites|toggle|join|leave|changeowner|info|kick|ban|unban>"));
                return;
            }

            switch (args[0].toLowerCase()) {
                case "create":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team create [teamName]"));
                        return;
                    }
                    String teamName = args[1];
                    if (teams.containsKey(teamName)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "A team with that name already exists."));
                        return;
                    }
                    String ownerName = player.getName();  // Get the owner's name
                    teams.put(teamName, new SimpleTeams.Team(teamName, playerUUID, ownerName));  // Store the name along with UUID
                    playerTeams.put(playerUUID, teamName);
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Team " + teamName + " created!"));
                    break;

                case "invite":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team invite [playerName]"));
                        return;
                    }
                    String inviteeName = args[1];
                    EntityPlayer invitee = server.getPlayerList().getPlayerByUsername(inviteeName);
                    if (invitee == null) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Player not found."));
                        return;
                    }

                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }
                    SimpleTeams.Team currentTeam = teams.get(playerTeams.get(playerUUID));

                    if (!currentTeam.isOwner(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not the team owner."));
                        return;
                    }

                    if (currentTeam.isPlayerBanned(invitee.getUniqueID())) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + invitee.getName() + " is banned from this team and cannot be invited."));
                        return;
                    }
                    pendingInvites.get(invitee.getUniqueID()).add(currentTeam.name);
                    invitee.sendMessage(new TextComponentString(TextFormatting.GOLD + "You have been invited to join team " + currentTeam.name + "."));
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Player invited."));
                    break;

                case "kick":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team kick <teamMember>"));
                        return;
                    }

                    String targetPlayerName = args[1];
                    EntityPlayer targetPlayer = server.getPlayerList().getPlayerByUsername(targetPlayerName);
                    if (targetPlayer == null) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Player not found."));
                        return;
                    }

                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }

                    currentTeam = teams.get(playerTeams.get(playerUUID));

                    if (!currentTeam.isOwner(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not the team owner."));
                        return;
                    }

                    if (currentTeam.isOwner(targetPlayer.getUniqueID())) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You cannot kick yourself, you are the team owner."));
                        return;
                    }

                    if (!SimpleTeams.Team.arePlayersInSameTeam(targetPlayer, player)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + targetPlayer.getName() + " is not in your team."));
                        return;
                    }

                    playerTeams.remove(targetPlayer.getUniqueID());
                    currentTeam.removeMember(targetPlayer.getUniqueID());

                    String kickMessage = TextFormatting.RED + targetPlayer.getName() + " has been kicked from the team.";

                    sendMessageToTeamMembers(server, kickMessage, currentTeam);

                    // Optionally notify the kicked player
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "You have been kicked from the team."));
                    }
                    break;

                case "invites":
                    List<String> invites = pendingInvites.get(playerUUID);
                    if (invites.isEmpty()) {
                        sender.sendMessage(new TextComponentString(TextFormatting.YELLOW + "You have no pending invites."));
                    } else {
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Pending invites: " + String.join(", ", invites)));
                    }
                    break;

                case "toggle":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team toggle [public|hurt]"));
                        return;
                    }
                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }
                    currentTeam = teams.get(playerTeams.get(playerUUID));
                    if (!currentTeam.isOwner(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not the team owner."));
                        return;
                    }
                    switch (args[1].toLowerCase()) {
                        case "public":
                            currentTeam.togglePublic();
                            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Team public status set to: " + currentTeam.isPublic));
                            break;
                        case "hurt":
                            currentTeam.toggleFriendlyFire();
                            sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Friendly fire set to: " + currentTeam.friendlyFire));
                            break;
                        default:
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown toggle option."));
                    }
                    break;

                case "join":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team join [teamName]"));
                        return;
                    }

                    teamName = args[1];
                    SimpleTeams.Team teamToJoin = teams.get(teamName);

                    if (playerTeams.getOrDefault(playerUUID, null) != null) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are already in a team!"));
                        return;
                    }

                    if (teamToJoin == null) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "That team does not exist."));
                        return;
                    }

                    if (!teamToJoin.isPublic && !pendingInvites.get(playerUUID).contains(teamName)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not invited to this team."));
                        return;
                    }

                    if (teamToJoin.isPlayerBanned(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are banned from this team and cannot join."));
                        return;
                    }

                    String joinMessage = TextFormatting.GREEN + player.getName() + " has joined the team.";

                    sendMessageToTeamMembers(server, joinMessage, teamToJoin);

                    playerTeams.put(playerUUID, teamName);
                    teamToJoin.addMember(playerUUID, player.getName());
                    pendingInvites.get(playerUUID).remove(teamName);
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "You have joined team " + teamName + "."));

                    break;

                case "leave":
                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }
                    currentTeam = teams.get(playerTeams.get(playerUUID));

                    // Check if the player is the team owner
                    if (currentTeam.isOwner(playerUUID)) {
                        long memberCount = playerTeams.values().stream().filter(teamName2 -> teamName2.equals(currentTeam.name)).count();

                        // Allow the owner to leave only if they are the only member
                        if (memberCount > 1) {
                            sender.sendMessage(new TextComponentString(TextFormatting.RED + "You cannot leave your own team unless you transfer ownership or remove all other members."));
                            return;
                        }

                        // Remove team and player mapping
                        teams.remove(currentTeam.name);
                        playerTeams.remove(playerUUID);
                        currentTeam.removeMember(playerUUID);  // Remove member from team
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "You have left and disbanded the team " + currentTeam.name + "."));
                    } else {
                        // Non-owner leaving the team
                        playerTeams.remove(playerUUID);
                        currentTeam.removeMember(playerUUID);  // Remove member from team
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "You have left the team."));
                    }
                    break;

                case "changeowner":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team changeowner [playerName]"));
                        return;
                    }
                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }
                    currentTeam = teams.get(playerTeams.get(playerUUID));
                    if (!currentTeam.isOwner(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not the team owner."));
                        return;
                    }
                    String newOwnerName = args[1];
                    EntityPlayer newOwner = server.getPlayerList().getPlayerByUsername(newOwnerName);
                    if (newOwner == null || !playerTeams.get(newOwner.getUniqueID()).equals(currentTeam.name)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "The specified player is not in your team."));
                        return;
                    }
                    currentTeam.setOwner(newOwner.getUniqueID(), newOwner.getName());  // Store new owner's name
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Ownership transferred to " + newOwner.getName() + "."));

                    String changeMessage = TextFormatting.GOLD + sender.getName() + " has transferred ownership of the team to " + newOwner.getName();

                    sendMessageToTeamMembers(server, changeMessage, currentTeam);

                    break;

                case "info":
                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }
                    currentTeam = teams.get(playerTeams.get(playerUUID));
                    List<String> teamMembers = new ArrayList<>();

                    // Gather all members of the team using stored member names (including offline players)
                    for (Map.Entry<UUID, String> entry : currentTeam.getMembers().entrySet()) {
                        teamMembers.add(entry.getValue());  // Use the stored name
                    }

                    // Get the owner's name (even if offline)
                    ownerName = currentTeam.getOwnerName();
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Team Name: " + TextFormatting.GREEN + currentTeam.name));
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Owner: " + TextFormatting.GREEN + ownerName));
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Public: " + TextFormatting.GREEN + currentTeam.isPublic));
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Friendly Fire: " + TextFormatting.GREEN + currentTeam.friendlyFire));
                    sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Members: " + TextFormatting.GREEN +String.join(", ", teamMembers)));
                    break;

                case "list":
                    List<String> publicTeams = new ArrayList<>();

                    for (Map.Entry<String, SimpleTeams.Team> entry : teams.entrySet()) {
                        SimpleTeams.Team team = entry.getValue();
                        if (team.isPublic) {
                            publicTeams.add(team.name);
                        }
                    }

                    if (publicTeams.isEmpty()) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "There are no public teams."));
                    } else {
                        sender.sendMessage(new TextComponentString(TextFormatting.GOLD + "Public Teams: " + TextFormatting.GREEN + String.join(", ", publicTeams)));
                    }
                    break;

                case "ban":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team ban <playerName>"));
                        return;
                    }


                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }

                    currentTeam = teams.get(playerTeams.get(playerUUID));
                    if (!currentTeam.isOwner(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not the team owner."));
                        return;
                    }

                    targetPlayerName = args[1];
                    targetPlayer = server.getPlayerList().getPlayerByUsername(targetPlayerName);
                    if (targetPlayer == null) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Player not found."));
                        return;
                    }

                    if (currentTeam.isOwner(targetPlayer.getUniqueID())) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You cannot ban yourself from the team, you are the team owner!"));
                        return;
                    }

                    String banMessage = TextFormatting.RED + targetPlayer.getName() + " has been kicked and banned from the team.";

                    if (SimpleTeams.Team.arePlayersInSameTeam(targetPlayer, player)) {
                        playerTeams.remove(targetPlayer.getUniqueID());
                        currentTeam.removeMember(targetPlayer.getUniqueID());
                        sendMessageToTeamMembers(server, banMessage, currentTeam);
                        targetPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "You have been kicked and banned from the team " + currentTeam.name + "."));
                    } else {
                        banMessage = TextFormatting.RED + targetPlayer.getName() + " has been banned from the team.";
                        sendMessageToTeamMembers(server, banMessage, currentTeam);
                    }

                    currentTeam.banPlayer(targetPlayer.getUniqueID());
                    break;

                case "unban":
                    if (args.length < 2) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Usage: /team unban <playerName>"));
                        return;
                    }

                    if (!playerTeams.containsKey(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not in a team."));
                        return;
                    }

                    currentTeam = teams.get(playerTeams.get(playerUUID));
                    if (!currentTeam.isOwner(playerUUID)) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "You are not the team owner."));
                        return;
                    }

                    targetPlayerName = args[1];
                    targetPlayer = server.getPlayerList().getPlayerByUsername(targetPlayerName);
                    if (targetPlayer == null) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Player not found."));
                        return;
                    }

                    if (!currentTeam.isPlayerBanned(targetPlayer.getUniqueID())) {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + targetPlayer.getName() + " is not banned from the team."));
                        return;
                    }

                    currentTeam.unbanPlayer(targetPlayer.getUniqueID());
                    sender.sendMessage(new TextComponentString(TextFormatting.GREEN + targetPlayer.getName() + " has been unbanned from the team."));

                    targetPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + "You have been unbanned from the team " + currentTeam.name + "."));

                    break;

                default:
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Unknown subcommand."));
            }
        }

        public static void sendMessageToTeamMembers(MinecraftServer server, String message, Team currentTeam) {
            // Iterate over all players in the team
            for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
                if (entry.getValue().equals(currentTeam.name)) {
                    EntityPlayer member = server.getPlayerList().getPlayerByUUID(entry.getKey());
                    if (member != null) {
                        member.sendMessage(new TextComponentString(message));
                    }
                }
            }
        }


    }

    public static class Team implements Serializable {
        private final String name;
        private UUID owner;
        private String ownerName;
        private boolean isPublic;
        private boolean friendlyFire;
        private final Map<UUID, String> members;
        private final List<UUID> bannedPlayers;

        public Team(String name, UUID owner, String ownerName) {
            this.name = name;
            this.owner = owner;
            this.ownerName = ownerName;
            this.isPublic = false;
            this.friendlyFire = false;
            this.members = new HashMap<>();
            this.members.put(owner, ownerName);  // Add the owner as a member initially
            this.bannedPlayers = new ArrayList<>();
        }

        public boolean isOwner(UUID uuid) {
            return owner.equals(uuid);
        }

        public void setOwner(UUID newOwner, String newOwnerName) {
            this.owner = newOwner;
            this.ownerName = newOwnerName;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void addMember(UUID memberUUID, String memberName) {
            members.put(memberUUID, memberName);
        }

        public void removeMember(UUID memberUUID) {
            members.remove(memberUUID);
        }

        public Map<UUID, String> getMembers() {
            return members;
        }

        public boolean isFriendlyFireEnabled() {
            return friendlyFire;
        }

        public void togglePublic() {
            this.isPublic = !this.isPublic;
        }

        public void toggleFriendlyFire() {
            this.friendlyFire = !this.friendlyFire;
        }

        public boolean isPlayerBanned(UUID playerUUID) {
            return bannedPlayers.contains(playerUUID);
        }

        public void banPlayer(UUID playerUUID) {
            if (!bannedPlayers.contains(playerUUID)) {
                bannedPlayers.add(playerUUID);
            }
        }

        public void unbanPlayer(UUID playerUUID) {
            bannedPlayers.remove(playerUUID);
        }

        public static boolean arePlayersInSameTeam(EntityPlayer player1, EntityPlayer player2) {
            String team1 = playerTeams.get(player1.getUniqueID());
            String team2 = playerTeams.get(player2.getUniqueID());
            return team1 != null && team1.equals(team2);
        }
    }
}
