package cz.zcu.fav.kiv.dobripet.communication;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;

/**
 * Created by Petr on 5/7/2017.
 */
public class TCItemPickedUp extends TCMessageData{

    private UnrealId id;

    public TCItemPickedUp(UnrealId id) {
        super(Tokens.get("TCItemPickedUp"));
        this.id = id;
    }

    public UnrealId getId() {
        return id;
    }

    public void setId(UnrealId id) {
        this.id = id;
    }

}