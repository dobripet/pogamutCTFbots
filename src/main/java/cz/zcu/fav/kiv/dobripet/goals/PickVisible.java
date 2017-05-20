package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;

/**
 * Created by Petr on 5/11/2017.
 */
public class PickVisible extends Goal {


    private Item item;
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
            //dont picked already picked weapon
            if(bot.getWeaponry().hasWeapon(item.getType())){
                continue;
            }
            double distance = bot.getPathDistance(visibleItem.getLocation());
            if( distance < DISTANCE){
                if(distance < minDistance){
                    minDistance = distance;
                    item = visibleItem;
                }
            }
        }
        if(item != null){
            bot.getLog().fine("PICKUP V SCORE " + Priority.VISIBLE_PRIORITY + " FOR ITEM " + item.getId());
            return Priority.VISIBLE_PRIORITY;
        }
        return 0d;
    }

    @Override
    public void abandon() {
        bot.getLog().fine("abandoning PickVisible");
        item = null;
    }
}