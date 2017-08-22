package cz.zcu.fav.kiv.dobripet.communication;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;
import cz.zcu.fav.kiv.dobripet.Role;

/**
 * Created by Petr on 5/10/2017.
 * Represents teammate bot state.
 */
public class TCTeammateInfo extends TCMessageData {

    private UnrealId botID;

    private Role role;

    private Location location;

    private UnrealId targetItemId;

    private String name;

    private boolean isSniper;

    private boolean isCamper;

    private double updatedTime;

    public TCTeammateInfo(UnrealId botID, Role role, Location location, UnrealId targetItemId, String name, boolean isSniper, boolean isCamper, double updatedTime) {
        super(Tokens.get("TCTeammateInfo"));
        this.botID = botID;
        this.role = role;
        this.location = location;
        this.targetItemId = targetItemId;
        this.name = name;
        this.isSniper = isSniper;
        this.isCamper = isCamper;
        this.updatedTime = updatedTime;
    }

    public UnrealId getBotID() {
        return botID;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UnrealId getTargetItemId() {
        return targetItemId;
    }

    public void setTargetItemId(UnrealId targetItemId) {
        this.targetItemId = targetItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSniper() {
        return isSniper;
    }

    public void setSniper(boolean sniper) {
        isSniper = sniper;
    }

    public boolean isCamper() {
        return isCamper;
    }

    public void setCamper(boolean camper) {
        isCamper = camper;
    }

    public double getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(double updatedTime) {
        this.updatedTime = updatedTime;
    }
}
