package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.zcu.fav.kiv.dobripet.*;
import cz.zcu.fav.kiv.dobripet.communication.TCTeammateInfo;

import java.awt.*;

/**
 * Created by Petr on 5/8/2017.
 */
public class Support extends Goal{


    private final double MATCHING_ROLE_PRIORITY = 50d;
    private final double UNMATCHING_ROLE_PRIORITY = 30d;
    private final double DISTANCE = 3000d;
    private final double CRITICAL_PRIORITY = 90d;

    //TODO stop support on kill
    private UnrealId supportingBotId;
    private SupportType type;

    public Support(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        bot.setSupporting(true);
        //defending carry
        if(type == SupportType.DEFEND_FLAG_CARRY) {
            TCTeammateInfo supportingBot = bot.getTeammates().get(supportingBotId);
            // stay close to support target
            if(supportingBotId == null){
                bot.getLog().severe("TOTAL ERROR SUPPORT 0");
            }
            if (bot.getPlayers().getPlayer(supportingBotId)==null){
                bot.getLog().severe("TOTAL ERROR SUPPORT");
            }
            if (bot.getPlayers().getPlayer(supportingBotId).isVisible()) {
                bot.followPlayer(bot.getPlayers().getPlayer(supportingBotId));
            } else {
                bot.goTo(bot.getTeammates().get(supportingBotId).getLocation());
            }
            //bot.draw(Color.cyan, supportingBot.getLocation());
            bot.getBot().getBotName().setInfo("SUPPORTING " + supportingBot.getName() + " D " + bot.getInfo().getLocation().getDistance(supportingBot.getLocation()) + " L" + supportingBot.getLocation());
        } else{
            //flag hunting

            bot.getBot().getBotName().setInfo("SUPPORT FLAG HUNTING");
            //at flag location but cant see flag
            if(bot.getInfo().atLocation(bot.getOurFlagLocation(),100) && !bot.getCTF().getOurFlag().isVisible()){
                bot.getLog().severe("OUR FLAG UNKNOWN POSITION");
                bot.setFlagUnknown(true);
            }
            //go to enemy base if unknown location
            if(bot.isFlagUnknown()){
                bot.goTo(bot.getCTF().getEnemyBase().getLocation());
            }else{
                //go get flag
                bot.goTo(bot.getOurFlagLocation());
            }
        }
    }

    @Override
    public double getPriority() {
        UnrealId chosenSupportId = null;
        double maxPriority = Double.MIN_VALUE;
        for (UnrealId botId : bot.getSupportRequests().keySet()){
            //cannot support myself
            if(botId == bot.getInfo().getId()){
                continue;
            }
            SupportType requestType = bot.getSupportRequests().get(botId);
            double distance = bot.getPathDistance(bot.getTeammates().get(botId).getLocation());
            if(bot.getRole() == Role.ATTACKER){
                if(requestType == SupportType.DEFEND_FLAG_CARRY ){
                    double priority = (DISTANCE / Utils.nonZeroDistance(distance)) * MATCHING_ROLE_PRIORITY;
                    if(priority > maxPriority){
                        maxPriority = priority;
                        chosenSupportId = botId;
                        type = requestType;
                    }
                }
            } else {
                if(requestType == SupportType.DEFEND_FLAG_CARRY ){
                    double priority = (DISTANCE / Utils.nonZeroDistance(distance)) * UNMATCHING_ROLE_PRIORITY;
                    if(priority > maxPriority){
                        maxPriority = priority;
                        chosenSupportId = botId;
                        type = requestType;
                    }
                }else if(requestType == SupportType.GET_OUR_FLAG){
                        supportingBotId = botId;
                        type = requestType;
                        bot.getLog().fine("SUPPORT SCORE " + CRITICAL_PRIORITY);
                        return CRITICAL_PRIORITY;
                    }
                }
            }
        bot.getLog().fine("SUPPORT SCORE " + maxPriority);
        supportingBotId = chosenSupportId;
        return maxPriority;
    }

    @Override
    public boolean hasFailed() {
        return false;
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public void abandon() {
        bot.setSupporting(false);
        bot.getLog().info("abandoning support");
        }

}
