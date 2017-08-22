package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.Utils;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * Created by Petr on 7/14/2017.
 * Goal to pick items.
 */
public class PickItem extends Goal {
    private Item item;

    public PickItem(CTFBot bot) {
        super(bot);
    }


    @Override
    public void perform() {
        //should not happened
        if(item == null){
            bot.getLog().warning("PickItem IS NULL!");
            return;
        }
        bot.getBot().getBotName().setInfo("PICK ITEM" + item.getType().getName());
        bot.pickItem(item);
    }

    @Override
    public double getPriority() {
        item = null;
        Set<Item> items = bot.getTaboo().filter(bot.getItems().getSpawnedItems().values());
        double maxScore = -1d;
        for (Item i : items) {
            double score = getItemScore(i);
            if(score > maxScore){
                maxScore = score;
                item = i;
            }
        }
        double distanceToPickUp = 500;
        //carry flag
        if(bot.getCTF().isBotCarryingEnemyFlag()){
            // our flag is home, no wandering
            if(bot.getCTF().isOurFlagHome()){
                distanceToPickUp *= 0.5d;
            }
            // else can wander little bit
            // distanceToPickUp *= 1d;
        } else if(!bot.getCTF().isOurFlagHome()){
            //our flag is taken, chase
            distanceToPickUp *= 0.25d;
        } else if(bot.getRole().equals(Role.DEFENDER)){
            //everything is calm, defender can wander
            distanceToPickUp *= 2d;
        } else if (bot.getInfo().atLocation(bot.getEnemyFlagLocation(), 3000)) {
            //attacker cannot wander near enemy base
            distanceToPickUp *=0.5d;
        } else if (bot.getInfo().atLocation(bot.getEnemyFlagLocation(), 1000)) {
            //attacker cannot wander at enemy base
            distanceToPickUp *=0.25d;
        }
        else{
            //attacker can wander
            distanceToPickUp *=3d;
        }
        double distance = bot.getPathDistance(item.getLocation());
        //if best item is in range, get him
        if(distanceToPickUp > distance){
            return Priority.PICK_ITEM;
        }
        //if bot has nothing to do, just pick best items
        return  1d;
    }

    @Override
    public void abandon() {
        bot.getLog().fine("abandoning PickItem");
        item = null;
        return;
    }

    public Item getItem() {
        return item;
    }


    private double getItemScore(Item item){
        double distanceMultiplier = 1d / Utils.nonZeroDistance(bot.getPathDistance(item.getLocation()));
        //weapon
        if(item.getDescriptor().getItemCategory().equals(ItemType.Category.WEAPON)){
            //no reason to pick up again
            if (bot.getWeaponry().hasWeapon(item.getType())) {
                return 0d;
            }
            //bot has no good weapon, should pick
            if (bot.getWeaponry().getWeapons().size() == 2){
                return 100d * distanceMultiplier;
            }
            //its ok to pick new weapon
            return 50d * distanceMultiplier;
        }
        //ammo
        if(item.getDescriptor().getItemCategory().equals(ItemType.Category.AMMO)) {
            ItemType weaponType =  Utils.getWeaponTypeFromAmmoType(item.getType());
            double currentAmmoRatio = ((double)(bot.getWeaponry().getMaxAmmo(item.getType()) - bot.getWeaponry().getAmmo(item.getType()))) / (double) bot.getWeaponry().getMaxAmmo(item.getType());
            if(bot.getWeaponry().hasWeapon(weaponType)){
                return 75d * currentAmmoRatio * distanceMultiplier;
            } else{
                return 25d * currentAmmoRatio *distanceMultiplier;
            }
        }
        //health
        if(item.getDescriptor().getItemCategory().equals(ItemType.Category.HEALTH)) {
            if(item.getType().equals(UT2004ItemType.HEALTH_PACK)){
                // no reason to pick health if full
                if(bot.getInfo().getHealth() >= 100){
                    return 0d;
                }
                double currentHealthRatio = (100d - bot.getInfo().getHealth())/100d;
                return 100d * currentHealthRatio * distanceMultiplier;
            }
            //health vials
            if(item.getType().equals(UT2004ItemType.MINI_HEALTH_PACK)){
                double currentHealthRatio = (100d - bot.getInfo().getHealth())/100d;
                return 1d * currentHealthRatio *distanceMultiplier;
            }
            //superhealth
            if(item.getType().equals(UT2004ItemType.SUPER_HEALTH_PACK)){
                double currentHealthRatio = (200d - bot.getInfo().getHealth())/200d;
                return 200d * currentHealthRatio * distanceMultiplier;
            }
        }
        //shield
        if(item.getDescriptor().getItemCategory().equals(ItemType.Category.SHIELD)) {
            if (item.getType().equals(UT2004ItemType.SHIELD_PACK)) {
                // no reason to pick shield if full
                if (bot.getInfo().getLowArmor() == 50) {
                    return 0d;
                }
                double currentShieldRatio = (50d - bot.getInfo().getLowArmor()) / 50d;
                return 50d * currentShieldRatio * distanceMultiplier;
            }
            if (item.getType().equals(UT2004ItemType.SUPER_SHIELD_PACK)) {
                //constant dependable on amount of all armor formula (d - (cur/max)*d)*PRIORITY*1.25
                double currentShieldRatio = (150d - bot.getInfo().getArmor()) / 150d;
                return 100d * currentShieldRatio * distanceMultiplier;
            }
        }
        //adrenaline
        if(item.getDescriptor().getItemCategory().equals(ItemType.Category.ADRENALINE)) {
            // no reason to pick adrenaline if full
            if(bot.getInfo().getAdrenaline() == 100 ) {
                return 0d;
            }
            return 25d * distanceMultiplier;
        }
        //double damage
        if(item.getType().equals(UT2004ItemType.U_DAMAGE_PACK)){
            return 150d * distanceMultiplier;
        }
        return 0d;
    }
}
