package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.SupportType;

/**
 * Created by Petr on 5/12/2017.
 */
public class HuntDown extends Goal{

    private final double DISTANCE = 3000;

    public HuntDown(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        //flag hunting
        bot.getBot().getBotName().setInfo("PICK FLAG");
        if(bot.getEnemyCarry().isVisible()) {
            bot.getBot().getBotName().setInfo("HUNT FLAG CARRY");
            bot.getLog().info("HUNT FLAG CARRY");
            bot.followPlayer(bot.getEnemyCarry());
            return;
        }
        //at flag location but cant see flag
        if(bot.getInfo().atLocation(bot.getOurFlagLocation(),100) && !bot.getCTF().getOurFlag().isVisible()){
            bot.getLog().severe("OUR FLAG UNKNOWN POSITION");
            bot.setFlagUnknown(true);
        }
        //go to enemy base if unknown location
        if(bot.isFlagUnknown()) {
            bot.goTo(bot.getCTF().getEnemyBase().getLocation());
            bot.getBot().getBotName().setInfo("FLAG UNKNOWN GOING TO ENEMY BASE");
            bot.getLog().info("FLAG UNKNOWN GOING TO ENEMY BASE");
            return;
        }
        //go to last known position
        bot.goTo(bot.getOurFlagLocation());
        bot.getBot().getBotName().setInfo("HUNT GOING TO LAST KNOWN OUR FLAG POSITION");
        bot.getLog().info("HUNT GOING TO LAST KNOWN OUR FLAG POSITION");
    }

    @Override
    public double getPriority() {
        //carry doesn't hunt
        if(!bot.getCTF().isBotCarryingEnemyFlag()){
            return 0d;
        }
        //enemy flag is safe
        for (UnrealId botId : bot.getSupportRequests().keySet()) {
            //cannot support myself
            if (botId == bot.getInfo().getId()) {
                continue;
            }
            if (bot.getSupportRequests().get(botId)== SupportType.GET_OUR_FLAG) {
                return Priority.HUNT_CRITICAL_PRIORITY;
            }
        }
        //hunt down our flag
        if(!bot.getCTF().isOurFlagHome()){
            if(bot.getCTF().getOurFlag().isVisible()){
                return Priority.HUNT_CRITICAL_PRIORITY;
            }
            return 30 + (DISTANCE - bot.getPathDistance(bot.getOurFlagLocation())) / DISTANCE * Priority.HUNT_PRIORITY;
        }
        return 0d;
    }

    @Override
    public void abandon() {

    }
}
