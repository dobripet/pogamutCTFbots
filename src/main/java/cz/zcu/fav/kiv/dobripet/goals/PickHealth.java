package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
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
public class PickHealth extends Goal {
    private Item item;
    private List<Item> itemsToPickUp;
    //maximal acceptable distance to return priority >= PRIORITY
    private final double DEFENDER_MAX_DISTANCE_TO_PRIORITY = 4000;
    private final double ATTACKER_MAX_DISTANCE_TO_PRIORITY = 8000;

    public PickHealth(CTFBot bot) {
        super(bot);
    }


    @Override
    public void perform() {
        bot.getBot().getBotName().setInfo("PICK HEALTH" + item.getType().getName());
        bot.pickItem(item);
    }

    @Override
    public double getPriority() {
        double maxPriority = 0;
        item = null;

        // no reason to pick health if full
        if(bot.getInfo().getHealth() == 200 ){
            return 0;
        }
        Set<Item> items = bot.getTaboo().filter(itemsToPickUp);
        for (Item i : items) {
            //acceptable distance to run
            double d = bot.getRole() == Role.ATTACKER ? ATTACKER_MAX_DISTANCE_TO_PRIORITY : DEFENDER_MAX_DISTANCE_TO_PRIORITY;
            //health pack
            double c = 0;
            if(i.getType().equals(UT2004ItemType.HEALTH_PACK)){
                // no reason to pick health if full
                if(bot.getInfo().getHealth() >= 100){
                    continue;
                }
                //constant dependable on amount of health formula (d - (cur/max)*d)*PRIORITY * 1.25
                c = (d - ((double)bot.getInfo().getHealth())/100 * d) * Priority.HEALTH_PRIORITY * 2;
            }else
            //health vials
            if(i.getType().equals(UT2004ItemType.MINI_HEALTH_PACK)){
                //constant dependable on amount of health formula (d - (cur/max)*d)*PRIORITY*0.75
                c = (d - ((double)bot.getInfo().getHealth())/200 * d) * Priority.HEALTH_PRIORITY * 0.75;
            }else

            //superhealth
            if(i.getType().equals(UT2004ItemType.SUPER_HEALTH_PACK)){
                //constant dependable on amount of health formula (d - (cur/max)*d)*PRIORITY*1.50
                c = (d - ((double)bot.getInfo().getHealth())/200 * d) * Priority.HEALTH_PRIORITY * 1.50;
            }
            //distance to item
            double distance = bot.getAStar().getDistance(bot.getInfo().getNearestNavPoint(), i.getNavPoint());
            //bot.getLog().fine("DISTANCE TO SPAWNED ITEM " + distance + " TYPE " + i.getType());
           double priority = c / Utils.nonZeroDistance(distance);
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
        if (maxPriority > Priority.HEALTH_MAX_PRIORITY) {
            return Priority.HEALTH_MAX_PRIORITY;
        }
        return maxPriority;
    }

    @Override
    public void abandon() {
        bot.getLog().fine("abandoning PickHeath");
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
