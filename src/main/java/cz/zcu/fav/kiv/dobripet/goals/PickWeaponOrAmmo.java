package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.UT2004Weaponry;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.Utils;

import java.util.*;

/**
 * Created by Petr on 4/14/2017.
 */
public class PickWeaponOrAmmo extends Goal {


    private Item item;
    private List<Item> itemsToPickUp;

    private List<ItemType> itemsTypesToPickUp;
    //priority
    private final double PRIORITY = 15;
    //maximal priority
    private final double MAX_PRIORITY = 50;
    //maximal acceptable distance to return priority >= PRIORITY
    private final double DEFENDER_MAX_DISTANCE_TO_PRIORITY = 5000;
    private final double ATTACKER_MAX_DISTANCE_TO_PRIORITY = 10000;

    public PickWeaponOrAmmo(CTFBot bot) {
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

        Set<Item> items = bot.getTaboo().filter(itemsToPickUp);
        System.out.println(items.size() + " " + itemsToPickUp.size());
        for (Item i : items) {
            ItemType weaponType;
            ItemType ammoType;
            double c = 0;
            //check if spawned item is ammo
            if (i.getDescriptor().getItemCategory().equals(ItemType.Category.AMMO)) {
                weaponType = Utils.getWeaponTypeFromAmmoType(i.getType());
                ammoType = i.getType();
            } else {
                weaponType = i.getType();
                ammoType = Utils.getAmmoTypeFromWeaponType(i.getType());
                //no reason to pick up again
                if (bot.getWeaponry().hasWeapon(weaponType)) {
                    continue;
                } else{
                    // give slight advantage to final score
                    c += 10000;
                }
            }
            //get distance
            if(i.getNavPoint() == null){
                bot.getLog().severe("ITEM NAV POInT IS NULL " +i);
                continue;
            }
            double distance = bot.getAStar().getDistance(bot.getInfo().getNearestNavPoint(), i.getNavPoint());
            //bot.getLog().fine("DISTANCE TO SPAWNED ITEM " + distance + " TYPE " + i.getType());
            //attacker has bigger distance
            double d = bot.getRole() == Role.ATTACKER ? ATTACKER_MAX_DISTANCE_TO_PRIORITY : DEFENDER_MAX_DISTANCE_TO_PRIORITY;
            //constant dependable on amount of ammo formula (d - (cur/max)*d)*PRIORITY
            c += (d - (((double) bot.getWeaponry().getAmmo(ammoType)) / bot.getWeaponry().getMaxAmmo(ammoType)) * d) * PRIORITY;
            //with low ammo bigger distance have bigger score, with almost full ammo score is low
            //System.out.println("dist " + distance + " c " +c);
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
        if (maxPriority > MAX_PRIORITY) {
            return MAX_PRIORITY*(2/bot.getWeaponry().getLoadedWeapons().size());
        }
        return maxPriority*(2/bot.getWeaponry().getLoadedWeapons().size());
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
        bot.getLog().fine("abandoning PickWeapon");
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

    public List<ItemType> getItemsTypesToPickUp() {
        return itemsTypesToPickUp;
    }

    public void setItemsTypesToPickUp(List<ItemType> itemsTypesToPickUp) {
        this.itemsTypesToPickUp = itemsTypesToPickUp;
    }
}