package cz.zcu.fav.kiv.dobripet.communication;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.token.Tokens;

/**
 * Created by Petr on 5/11/2017.
 * Represents enemy bot state.
 */
public class TCEnemyInfo extends TCMessageData {

    private UnrealId botID;

    private Location location;

    private boolean isCarry;

    private double updatedTime;


    public TCEnemyInfo(UnrealId botID, Location location, boolean isCarry, double updatedTime) {
        super(Tokens.get("TCEnemyInfo"));
        this.botID = botID;
        this.location = location;
        this.isCarry = isCarry;
        this.updatedTime = updatedTime;
    }

    public UnrealId getBotID() {
        return botID;
    }

    public double getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(double updatedTime) {
        this.updatedTime = updatedTime;
    }

    public void setBotID(UnrealId botID) {
        this.botID = botID;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isCarry() {
        return isCarry;
    }

    public void setCarry(boolean carry) {
        isCarry = carry;
    }

}
