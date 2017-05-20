package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.Role;

import java.util.List;

/**
 * Created by Petr on 5/8/2017.
 */
public class DefendOurFlag extends Goal {
    //sniping spots
    private List<Location> defendSpots;
    //sniping focus spots
    private List<Location> defendFocusSpots;
    Location defendSpot;

    private int roundIndex = 0;

    public DefendOurFlag(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        //hunting
        if(!bot.getCTF().isOurFlagHome()){
            bot.getBot().getBotName().setInfo("DEFEND FLAG HUNTING");
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
        } else{
            //camping
            bot.setCamper(true);
            bot.goTo(defendSpot);
            //already near spot
            if (bot.getInfo().getLocation().getDistance(defendSpot) < 200) {
                //look around
                if (bot.getEnemy() == null) {
                    Location focus = defendFocusSpots.get(roundIndex);
                    roundIndex++;
                    if (roundIndex == defendFocusSpots.size()) {
                        roundIndex = 0;
                    }
                    bot.getNavigation().setFocus(focus);
                    bot.getBot().getBotName().setInfo("DEFENDER LOOKING FOR TARGETS " + focus);
                    bot.getLog().fine("DEFENDER LOOKING FOR TARGETS " + bot.getNavPoints().getVisibleNavPoints().size());
                } else {
                    bot.getBot().getBotName().setInfo("DEFENDING " + bot.getEnemy().getName());
                }
            } else {
                bot.getBot().getBotName().setInfo("GOING TO DEFEND SPOT" + defendSpot);
            }
        }

    }

    @Override
    public double getPriority() {
        defendSpot = null;
        if(bot.getCTF().isBotCarryingEnemyFlag()){
            return 0d;
        }
        if(bot.getRole() == Role.DEFENDER){
            if(!bot.getCTF().isOurFlagHome()){
                if(bot.getCTF().getOurFlag().isVisible()){
                    return Priority.DEFEND_FLAG_PRIORITY;
                }else{
                    return Priority.DEFEND_HUNT_DEFENDER_PRIORITY;
                }
            } else {
                defendSpot = defendSpots.get(roundIndex % defendSpots.size());
                if(bot.isAnybodyCamper()){
                    return 0d;
                }
                return Priority.DEFEND_CAMP_PRIORITY;
            }
        } else{
            //attacker
            if(!bot.getCTF().isOurFlagHome()){
                if(bot.getCTF().getOurFlag().isVisible()){
                    return Priority.DEFEND_FLAG_PRIORITY;
                }else{
                    return Priority.DEFEND_HUNT_ATTACKER_PRIORITY;
                }
            } else {
                return 0d;
            }
        }
    }

    @Override
    public void abandon() {
        defendSpot = null;
    }

    public List<Location> getDefendSpots() {
        return defendSpots;
    }

    public void setDefendSpots(List<Location> defendSpots) {
        this.defendSpots = defendSpots;
    }

    public List<Location> getDefendFocusSpots() {
        return defendFocusSpots;
    }

    public void setDefendFocusSpots(List<Location> defendFocusSpots) {
        this.defendFocusSpots = defendFocusSpots;
    }
}
