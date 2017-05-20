package cz.zcu.fav.kiv.dobripet;

import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemTypeTranslator;
import net.sf.saxon.type.ListType;

import java.util.*;

/**
 * Created by Petr on 4/16/2017.
 */
public class Utils {
    private static Map<ItemType, ItemType> weaponToAmmo;
    private static Map<ItemType, ItemType> ammoToWeapon;

    static
    {
        weaponToAmmo = new HashMap<ItemType, ItemType>();
        weaponToAmmo.put(UT2004ItemType.ASSAULT_RIFLE, UT2004ItemType.ASSAULT_RIFLE_AMMO);
        weaponToAmmo.put(UT2004ItemType.BIO_RIFLE, UT2004ItemType.BIO_RIFLE_AMMO);
        weaponToAmmo.put(UT2004ItemType.FLAK_CANNON, UT2004ItemType.FLAK_CANNON_AMMO);
        weaponToAmmo.put(UT2004ItemType.LIGHTNING_GUN, UT2004ItemType.LIGHTNING_GUN_AMMO);
        weaponToAmmo.put(UT2004ItemType.LINK_GUN, UT2004ItemType.LINK_GUN_AMMO);
        weaponToAmmo.put(UT2004ItemType.MINIGUN, UT2004ItemType.MINIGUN_AMMO);
        weaponToAmmo.put(UT2004ItemType.ROCKET_LAUNCHER, UT2004ItemType.ROCKET_LAUNCHER_AMMO);
        weaponToAmmo.put(UT2004ItemType.SHOCK_RIFLE, UT2004ItemType.SHOCK_RIFLE_AMMO);
        weaponToAmmo.put(UT2004ItemType.SNIPER_RIFLE, UT2004ItemType.SNIPER_RIFLE_AMMO);

        ammoToWeapon = new HashMap<ItemType, ItemType>();
        ammoToWeapon.put(UT2004ItemType.ASSAULT_RIFLE_AMMO, UT2004ItemType.ASSAULT_RIFLE);
        ammoToWeapon.put(UT2004ItemType.BIO_RIFLE_AMMO, UT2004ItemType.BIO_RIFLE);
        ammoToWeapon.put(UT2004ItemType.FLAK_CANNON_AMMO, UT2004ItemType.FLAK_CANNON);
        ammoToWeapon.put(UT2004ItemType.LIGHTNING_GUN_AMMO, UT2004ItemType.LIGHTNING_GUN);
        ammoToWeapon.put(UT2004ItemType.LINK_GUN_AMMO, UT2004ItemType.LINK_GUN);
        ammoToWeapon.put(UT2004ItemType.MINIGUN_AMMO, UT2004ItemType.MINIGUN);
        ammoToWeapon.put(UT2004ItemType.ROCKET_LAUNCHER_AMMO, UT2004ItemType.ROCKET_LAUNCHER);
        ammoToWeapon.put(UT2004ItemType.SHOCK_RIFLE_AMMO, UT2004ItemType.SHOCK_RIFLE);
        ammoToWeapon.put(UT2004ItemType.SNIPER_RIFLE_AMMO, UT2004ItemType.SNIPER_RIFLE);
    }

    public static ItemType getAmmoTypeFromWeaponType(ItemType weaponType){
        return weaponToAmmo.get(weaponType);
    }

    public static ItemType getWeaponTypeFromAmmoType(ItemType ammoType){
        return ammoToWeapon.get(ammoType);
    }

    public static void addAmmoTypeFromWeaponTypes(Set<ItemType> types){
        Set<ItemType> ammo = new HashSet<ItemType>();
        for(ItemType type : types){
            if(type.getCategory().equals(ItemType.Category.WEAPON)){
                ammo.add(ammoToWeapon.get(type));
            }
        }
    }

    public static double nonZeroDistance(double distance){
        if(distance != 0){
            return distance;
        }
        return 1d;
    }
}
