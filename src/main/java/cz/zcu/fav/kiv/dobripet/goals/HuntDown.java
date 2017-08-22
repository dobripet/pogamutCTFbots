package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.zcu.fav.kiv.dobripet.*;

/**
 * Created by Petr on 5/12/2017.
 * Goal to hunt our flag.
 */
public class HuntDown extends Goal{

    private Location tempTarget;

    public HuntDown(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        if(bot.isFlagUnknown()){
            //at enemy base, roam around
            if(bot.getInfo().atLocation(bot.getCTF().getEnemyBase().getLocation(), 100) && !bot.getCTF().getOurFlag().isVisible()){
                tempTarget = bot.getNavPoints().getRandomNavPoint().getLocation();
            }
            //at roam target choose new
            if(bot.getInfo().atLocation(tempTarget, 100) && !bot.getCTF().getOurFlag().isVisible()){
                tempTarget = bot.getNavPoints().getRandomNavPoint().getLocation();
            }
            //has roam target, go roaming
            if(tempTarget != null){
                bot.goTo(tempTarget);
                bot.getLog().warning("ROAM - OUR FLAG UNKNOWN POSITION");
                return;
            }
            //no roam target yet, go to enemy base
            bot.getLog().warning("GO ENEMY BASE - OUR FLAG UNKNOWN POSITION");
            bot.goTo(bot.getCTF().getEnemyBase().getLocation());
            return;
        }
        //bot is on supposed location, but flag is not there
        if(bot.getInfo().atLocation(bot.getOurFlagLocation(), 100) && !bot.getCTF().getOurFlag().isVisible()){
            bot.setFlagUnknown(true);
            bot.getLog().warning("OUR FLAG UNKNOWN POSITION");
            bot.goTo(bot.getCTF().getEnemyBase().getLocation());
            return;
        }
        //flag pickup
        if(bot.getCTF().isOurFlagDropped()){
            bot.getBot().getBotName().setInfo("PICK OUR FLAG");
            bot.getLog().info("PICK OUR FLAG");
            bot.goTo(bot.getOurFlagLocation());
            return;
        }
        //hunt down enemy carry
        if (bot.getEnemyCarry() != null && bot.getEnemyCarry().isVisible()) {
            //adrenaline
            if(bot.getCombo().canPerformCombo()){
                bot.getCombo().performBerserk();
            }
            bot.getBot().getBotName().setInfo("HUNT ENEMY FLAG CARRY");
            bot.getLog().info("HUNT ENEMY FLAG CARRY");
            bot.followPlayer(bot.getEnemyCarry());
            return;
        }
        //else just go to last known position
        bot.goTo(bot.getOurFlagLocation());
        bot.getBot().getBotName().setInfo("HUNT GOING TO LAST KNOWN OUR FLAG POSITION");
        bot.getLog().info("HUNT GOING TO LAST KNOWN OUR FLAG POSITION");
    }

    @Override
    public double getPriority() {
        //is our flag dropped, go pick it
        if (bot.getCTF().isOurFlagDropped() && !bot.isFlagUnknown() && Utils.isNearest(bot, bot.getOurFlagLocation())) {
            return Priority.HUNT_PICK_FLAG_PRIORITY;
        }
        //carry doesn't hunt
        if (bot.getCTF().isBotCarryingEnemyFlag()) {
            return 0d;
        }
        //enemy flag is safe
        for (UnrealId botId : bot.getSupportRequests().keySet()) {
            //cannot support myself
            if (botId == bot.getInfo().getId()) {
                continue;
            }
            if (bot.getSupportRequests().get(botId) == SupportType.GET_OUR_FLAG) {
                return Priority.HUNT_CRITICAL_PRIORITY;
            }
        }
        //hunt down our flag
        if (!bot.getCTF().isOurFlagHome()) {
            if (bot.getCTF().getOurFlag().isVisible() || bot.getRole().equals(Role.DEFENDER)) {
                return Priority.HUNT_CRITICAL_PRIORITY;
            }
            return Priority.HUNT_PRIORITY;
        }
        return 0d;
    }

    @Override
    public void abandon() {
        tempTarget = null;
    }
}
