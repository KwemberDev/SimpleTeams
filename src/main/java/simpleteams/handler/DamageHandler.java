package simpleteams.handler;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import simpleteams.SimpleTeams;

import static simpleteams.SimpleTeams.playerTeams;
import static simpleteams.SimpleTeams.teams;

@SideOnly(Side.SERVER)
public class DamageHandler {
    @SubscribeEvent
    public void onEntityAttack(LivingAttackEvent event) {
        if (event.getSource().getTrueSource() instanceof EntityPlayer &&
                event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer attacker = (EntityPlayer) event.getSource().getTrueSource();
            String team = playerTeams.getOrDefault(attacker.getUniqueID(), null);
            if (team != null && !teams.get(team).isFriendlyFireEnabled()) {
                EntityPlayer target = (EntityPlayer) event.getEntityLiving();
                if (SimpleTeams.Team.arePlayersInSameTeam(attacker, target)) {
                    event.setCanceled(true); // Cancel damage if in the same team
                }
            }
        }
    }
}
