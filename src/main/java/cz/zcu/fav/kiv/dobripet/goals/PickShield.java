package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.Utils;

import java.util.List;
import java.util.Set;

/**
 * Created by Petr on 4/17/2017.
 */
public class PickShield extends Goal{
    private Item item;
    private List<Item> itemsToPickUp;
    //priority
    private final double PRIORITY = 10;
    //maximal priority
    private final double MAX_PRIORITY = 50;
    //maximal acceptable distance to return priority >= PRIORITY
    private final double DEFENDER_MAX_DISTANCE_TO_PRIORITY = 5000;
    private final double ATTACKER_MAX_DISTANCE_TO_PRIORITY = 10000;

    public PickShield(CTFBot bot) {
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

        // no reason to pick armor if full
        if(bot.getInfo().getArmor() == 150 ){
            return 0;
        }
        Set<Item> items = bot.getTaboo().filter(itemsToPickUp);
        System.out.println(items.size() + " " + itemsToPickUp.size());
        for (Item i : items) {
            //acceptable distance to run
            double d = bot.getRole() == Role.ATTACKER ? ATTACKER_MAX_DISTANCE_TO_PRIORITY : DEFENDER_MAX_DISTANCE_TO_PRIORITY;
            //small shield pack
            double c = 0;
            if(i.getType().equals(UT2004ItemType.SHIELD_PACK)){
                // no reason to pick armor if full
                if(bot.getInfo().getLowArmor() == 50){
                    continue;
                }
                //constant dependable on amount of low armor formula (d - (cur/max)*d)*PRIORITY
                c = (d - ((double)bot.getInfo().getLowArmor())/50 * d) * PRIORITY;
            }
            if(i.getType().equals(UT2004ItemType.SUPER_SHIELD_PACK)){
                //constant dependable on amount of all armor formula (d - (cur/max)*d)*PRIORITY*1.25
                c = (d - ((double)bot.getInfo().getArmor())/150 * d) * PRIORITY * 1.25;
            }
            //relative health modification variable
            double health = 2 * ((double)bot.getInfo().getHealth())/100;
            //distance to item
            double distance = bot.getAStar().getDistance(bot.getInfo().getNearestNavPoint(), i.getNavPoint());
            //bot.getLog().info("DISTANCE TO SPAWNED ITEM " + distance + " TYPE " + i.getType());
            double priority = (c / health) / Utils.nonZeroDistance(distance);
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
        if (maxPriority > MAX_PRIORITY) {
            return MAX_PRIORITY;
        }
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
        bot.getLog().fine("abandoning PickShield");
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

