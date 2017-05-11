package cz.zcu.fav.kiv.dobripet.communication;

import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;
import cz.zcu.fav.kiv.dobripet.Role;


/**
 * Created by Petr on 5/7/2017.
 */
public class TCChangeRole extends TCMessageData{

    private Role role;

    public TCChangeRole(Role role) {
        super(Tokens.get("TCChangeRole"));
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

}
