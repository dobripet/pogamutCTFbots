package cz.zcu.fav.kiv.dobripet.communication;

import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;
import cz.zcu.fav.kiv.dobripet.SupportType;

/**
 * Created by Petr on 5/10/2017.
 * Represents support state
 */
public class TCSupportUpdate extends TCMessageData {
    private UnrealId botId;
    private SupportType supportType;
    private boolean revoking;

    public TCSupportUpdate(UnrealId botId, SupportType supportType, boolean revoking) {
        super(Tokens.get("TCSupportUpdate"));
        this.botId = botId;
        this.supportType = supportType;
        this.revoking = revoking;
    }

    public UnrealId getBotId() {
        return botId;
    }

    public void setBotId(UnrealId botId) {
        this.botId = botId;
    }

    public SupportType getSupportType() {
        return supportType;
    }

    public void setSupportType(SupportType supportType) {
        this.supportType = supportType;
    }

    public boolean isRevoking() {
        return revoking;
    }

    public void setRevoking(boolean revoking) {
        this.revoking = revoking;
    }
}