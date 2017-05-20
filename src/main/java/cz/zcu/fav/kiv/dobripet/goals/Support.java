package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.zcu.fav.kiv.dobripet.*;
import cz.zcu.fav.kiv.dobripet.communication.TCTeammateInfo;

import java.awt.*;

/**
 * Created by Petr on 5/8/2017.
 */
public class Support extends Goal{


    private final double DISTANCE = 2000d;

    private UnrealId supportingBotId;
    private SupportType type;

    public Support(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        bot.setSupporting(true);
        //defending carry
        TCTeammateInfo supportingBot = bot.getTeammates().get(supportingBotId);
        // stay close to support target
        if(supportingBotId == null){
            bot.getLog().severe("TOTAL ERROR SUPPORT 0");
        }
        if (bot.getPlayers().getPlayer(supportingBotId)==null){
            bot.getLog().severe("TOTAL ERROR SUPPORT " + supportingBotId);
        }
        if (bot.getPlayers().getPlayer(supportingBotId).isVisible()) {
            bot.followPlayer(bot.getPlayers().getPlayer(supportingBotId));
        } else {
            bot.goTo(bot.getTeammates().get(supportingBotId).getLocation());
        }
        //bot.draw(Color.cyan, supportingBot.getLocation());
        bot.getBot().getBotName().setInfo("SUPPORTING " + supportingBot.getName() + " D " + bot.getInfo().getLocation().getDistance(supportingBot.getLocation()) + " L" + supportingBot.getLocation());
    }

    @Override
    public double getPriority() {
        UnrealId chosenSupportId = null;
        double maxPriority = Double.MIN_VALUE;
        for (UnrealId botId : bot.getSupportRequests().keySet()) {
            //cannot support myself
            if (botId == bot.getInfo().getId()) {
                continue;
            }
            SupportType requestType = bot.getSupportRequests().get(botId);
            double distance = bot.getPathDistance(bot.getTeammates().get(botId).getLocation());
            if (requestType == SupportType.DEFEND_FLAG_CARRY) {
                double priority;
                if (bot.getRole() == Role.ATTACKER) {
                    priority = (DISTANCE / Utils.nonZeroDistance(distance)) * Priority.SUPPORT_MATCHING_PRIORITY;
                } else {
                    priority = (DISTANCE / Utils.nonZeroDistance(distance)) * Priority.SUPPORT_UNMATCHING_PRIORITY;
                }
                if (priority > maxPriority) {
                    maxPriority = priority;
                    chosenSupportId = botId;
                    type = requestType;
                }
            }
        }
        bot.getLog().fine("SUPPORT SCORE " + maxPriority);
        supportingBotId = chosenSupportId;
        return maxPriority;
    }

    @Override
    public void abandon() {
        bot.setSupporting(false);
        supportingBotId = null;
        bot.getLog().info("abandoning support");
        }

}
