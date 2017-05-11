package cz.zcu.fav.kiv.dobripet;

import cz.cuni.amis.pogamut.ut2004.teamcomm.server.UT2004TCServer;

/**
 * Created by Petr on 5/9/2017.
 */
public class TCServerStarter {
    public static void main(String[] args) {
        // Start TC (~ TeamCommunication) Server first...
        UT2004TCServer tcServer = UT2004TCServer.startTCServer();
    }
}
