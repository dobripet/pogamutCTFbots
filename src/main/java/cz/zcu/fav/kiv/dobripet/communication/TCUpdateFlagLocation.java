package cz.zcu.fav.kiv.dobripet.communication;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;

/**
 * Created by Petr on 5/10/2017.
 */
public class TCUpdateFlagLocation extends TCMessageData {
    private Location enemyFlagLocation;
    private Location ourFlagLocation;


    public TCUpdateFlagLocation(Location enemyFlagLocation, Location ourFlagLocation) {
        super(Tokens.get("TCUpdateFlagLocation"));
        this.enemyFlagLocation = enemyFlagLocation;
        this.ourFlagLocation = ourFlagLocation;
    }

    public Location getEnemyFlagLocation() {
        return enemyFlagLocation;
    }

    public void setEnemyFlagLocation(Location enemyFlagLocation) {
        this.enemyFlagLocation = enemyFlagLocation;
    }

    public Location getOurFlagLocation() {
        return ourFlagLocation;
    }

    public void setOurFlagLocation(Location ourFlagLocation) {
        this.ourFlagLocation = ourFlagLocation;
    }

    @Override
    public String toString() {
        return "TCUpdateFlagLocation{" +
                "enemyFlagLocation=" + enemyFlagLocation +
                ", ourFlagLocation=" + ourFlagLocation +
                '}';
    }
}