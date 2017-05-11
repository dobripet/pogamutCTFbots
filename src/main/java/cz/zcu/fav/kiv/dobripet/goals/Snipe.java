package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Role;

import java.util.List;
import java.util.Random;

/**
 * Created by Petr on 5/11/2017.
 */
public class Snipe extends Goal {
    //sniping spots
    private List<Location> snipingSpots;
    //sniping focus spots
    private List<Location> snipingFocusSpots;
    Location snipingSpot;

    private int roundIndex = 0;

    public Snipe(CTFBot bot) {
        super(bot);
    }

    @Override
    public void perform() {
        bot.setSniper(true);
        bot.goTo(snipingSpot);
        //already near spot
        if (bot.getInfo().getLocation().getDistance(snipingSpot) < 200) {
            //bot.setCamping(true);
            //look around
            if (bot.getEnemy() == null) {
                Location focus = snipingFocusSpots.get(roundIndex);
                roundIndex++;
                if (roundIndex == snipingFocusSpots.size()) {
                    roundIndex = 0;
                }
                bot.getNavigation().setFocus(focus);
                bot.getBot().getBotName().setInfo("SNIPER LOOKING FOR TARGETS " + focus);
                bot.getLog().fine("SNIPER LOOKING FOR TARGETS " + bot.getNavPoints().getVisibleNavPoints().size());
            } else {
                bot.getBot().getBotName().setInfo("SNIPING " + bot.getEnemy().getName());
            }
        } else {
            bot.getBot().getBotName().setInfo("GOING TO SNIPE SPOT" + snipingSpot);
        }
    }

    @Override
    public double getPriority() {
        if(bot.getCTF().isOurFlagHome()){
            if(!bot.isAnybodySniper() && bot.hasSniperGun()){
                //rnd sniping spot
                snipingSpot = snipingSpots.get(roundIndex % snipingSpots.size());
                if(bot.getRole() == Role.ATTACKER){
                    return 40d;
                }
                return 44d;
            }
        }
        return 0d;
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
        if(bot.getEnemy() == null) {
            bot.getNavigation().setFocus(null);
        }
    }

    public List<Location> getSnipingSpots() {
        return snipingSpots;
    }

    public void setSnipingSpots(List<Location> snipingSpots) {
        this.snipingSpots = snipingSpots;
    }

    public List<Location> getSnipingFocusSpots() {
        return snipingFocusSpots;
    }

    public void setSnipingFocusSpots(List<Location> snipingFocusSpots) {
        this.snipingFocusSpots = snipingFocusSpots;
    }
}
