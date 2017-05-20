package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.Utils;

import java.util.List;
import java.util.Set;

/**
 * Created by Petr on 4/17/2017.
 */
public class PickAdrenaline extends Goal {

    private Item item;
    private List<Item> itemsToPickUp;
    //maximal acceptable distance to return priority >= PRIORITY
    private final double DEFENDER_MAX_DISTANCE_TO_PRIORITY = 2000 * Priority.ADRENALINE_PRIORITY;
    private final double ATTACKER_MAX_DISTANCE_TO_PRIORITY = 4000 * Priority.ADRENALINE_PRIORITY;

    public PickAdrenaline(CTFBot bot) {
        super(bot);
    }


    @Override
    public void perform() {
        bot.getBot().getBotName().setInfo("PICK " + item.getType().getName());
        bot.pickItem(item);
    }

    @Override
    public double getPriority() {
        double maxPriority = 0;
        item = null;

        // no reason to pick adrenaline if full
        if(bot.getInfo().getAdrenaline() == 100 ) {
            return 0;
        }
        Set<Item> items = bot.getTaboo().filter(itemsToPickUp);
        for (Item i : items) {
            if(i == null ){
                bot.getLog().severe("nechapu, taboo bylo null");
                continue;
            }
            //acceptable distance to run
            double d = bot.getRole() == Role.ATTACKER ? ATTACKER_MAX_DISTANCE_TO_PRIORITY : DEFENDER_MAX_DISTANCE_TO_PRIORITY;
            //distance to item
            double distance = bot.getAStar().getDistance(bot.getInfo().getNearestNavPoint(), i.getNavPoint());
            //bot.getLog().fine("DISTANCE TO SPAWNED ITEM " + distance + " TYPE " + i.getType());
            double priority = d / Utils.nonZeroDistance(distance);
            if (maxPriority < priority) {
                maxPriority = priority;
                item = i;
                //bot.getLog().severe("MAX AMMO : " + bot.getWeaponry().getMaxAmmo(ammoType) + " AMMO " + bot.getWeaponry().getAmmo(ammoType) + " TYPE " + i.getType());
            }


        }
        if (item != null) {
            bot.getLog().fine("PICKUP SCORE " + maxPriority + " FOR ITEM " + item.getId());

        } else {
            bot.getLog().fine("NO ITEM TO PICK");
        }
        // hard limit
        if (maxPriority > Priority.ADRENALINE_MAX_PRIORITY) {
            return Priority.ADRENALINE_MAX_PRIORITY;
        }
        return maxPriority;
    }

    @Override
    public void abandon() {
        bot.getLog().fine("abandoning GetItems");
        item = null;
        return;
    }

    public Item getItem() {
        return item;
    }

    public List<Item> getItemsToPickUp() {
        return itemsToPickUp;
    }

    public void setItemsToPickUp(List<Item> itemsToPickUp) {
        this.itemsToPickUp = itemsToPickUp;
    }
}