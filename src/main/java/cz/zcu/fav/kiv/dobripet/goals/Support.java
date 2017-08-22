package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.zcu.fav.kiv.dobripet.*;
import cz.zcu.fav.kiv.dobripet.communication.TCTeammateInfo;

import java.awt.*;

/**
 * Created by Petr on 5/8/2017.
 * Goal to support our carry.
 */
public class Support extends Goal{

    private UnrealId supportingBotId;
    private SupportType type;

    public Support(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        //should not happened
        if(supportingBotId == null || bot.getPlayers().getPlayer(supportingBotId)==null){
            bot.getLog().severe("SUPPORT NULL");
            return;
        }
        //defending carry
        TCTeammateInfo supportingBot = bot.getTeammates().get(supportingBotId);
        // stay close to support target
        if (bot.getPlayers().getPlayer(supportingBotId).isVisible()) {
            bot.followPlayer(bot.getPlayers().getPlayer(supportingBotId));
        } else {
            if(bot.getTeammates().get(supportingBotId) == null){
                bot.getLog().severe("SUPPORT TEAMMATE NULL");
                return;
            }
            bot.goTo(bot.getTeammates().get(supportingBotId).getLocation());
        }
        bot.setSupporting(true);
        bot.getLog().fine("SUPPORTING " + supportingBot.getName() + " D " + bot.getInfo().getLocation().getDistance(supportingBot.getLocation()) + " L" + supportingBot.getLocation());
        //bot.draw(Color.cyan, supportingBot.getLocation());
        bot.getBot().getBotName().setInfo("SUPPORTING " + supportingBot.getName() + " D " + bot.getInfo().getLocation().getDistance(supportingBot.getLocation()) + " L" + supportingBot.getLocation());
    }

    @Override
    public double getPriority() {
        double maxPriority = -1d;
        for (UnrealId botId : bot.getSupportRequests().keySet()) {
            //cannot support myself
            if (botId == bot.getInfo().getId()) {
                continue;
            }
            SupportType requestType = bot.getSupportRequests().get(botId);
            double distance = bot.getPathDistance(bot.getTeammates().get(botId).getLocation());
            if (requestType == SupportType.DEFEND_FLAG_CARRY && distance < 3500) {
                supportingBotId = botId;
                type = requestType;
                if (bot.getRole() == Role.ATTACKER) {
                    return Priority.SUPPORT_ATTACKER_PRIORITY;
                } else {
                    return Priority.SUPPORT_DEFENDER_PRIORITY;
                }
            }
        }
        return maxPriority;
    }

    @Override
    public void abandon() {
        bot.setSupporting(false);
        supportingBotId = null;
        bot.getLog().info("abandoning support");
        }

}
