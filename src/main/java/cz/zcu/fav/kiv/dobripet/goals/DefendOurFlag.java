package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.Utils;

import java.util.List;

/**
 * Created by Petr on 5/8/2017.
 * Goal to camp at our flag.
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
        bot.setCamper(true);
        //already near spot
        if (bot.getInfo().atLocation(defendSpot, 300)) {
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
                //defend
                bot.goTo(bot.getEnemy());
                bot.getBot().getBotName().setInfo("DEFENDING " + bot.getEnemy().getName());
            }
        }else {
            bot.getBot().getBotName().setInfo("GOING TO DEFEND SPOT" + defendSpot);
            bot.goTo(defendSpot);
        }
    }

    @Override
    public double getPriority() {
        if(bot.getCTF().isBotCarryingEnemyFlag()){
            return 0d;
        }
        defendSpot = defendSpots.get(roundIndex % defendSpots.size());
        //our flag has to be home, bot has to be defender and be the only one camping
        if(bot.getCTF().isOurFlagHome() && bot.getRole() == Role.DEFENDER && !bot.isAnybodyCamper()){
            return Priority.DEFEND_FLAG_PRIORITY;
        }
        return 0d;
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
