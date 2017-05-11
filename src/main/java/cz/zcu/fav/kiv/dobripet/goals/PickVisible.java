package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.zcu.fav.kiv.dobripet.CTFBot;

/**
 * Created by Petr on 5/11/2017.
 */
public class PickVisible extends Goal {


    private Item item;
    //priority
    private final double PRIORITY = 100d;
    //maximal distance
    private final double DISTANCE = 500;

    public PickVisible(CTFBot bot) {
        super(bot);
    }


    @Override
    public void perform() {
        bot.getBot().getBotName().setInfo("PICK VISIBLE" + item.getType().getName());
        bot.pickItem(item);
    }

    @Override
    public double getPriority() {
        double minDistance = Double.MAX_VALUE;
        item = null;
        for(Item visibleItem : bot.getItems().getVisibleItems().values()){
            double distance = bot.getPathDistance(visibleItem.getLocation());
            if( distance < DISTANCE){
                if(distance < minDistance){
                    minDistance = distance;
                    item = visibleItem;
                }
            }
        }
        if(item != null){
            bot.getLog().fine("PICKUP SCORE " + PRIORITY + " FOR ITEM " + item.getId());
            return PRIORITY;
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
        bot.getLog().fine("abandoning PickVisible");
        item = null;
    }
}