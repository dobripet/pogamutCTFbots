package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.SupportType;
import cz.zcu.fav.kiv.dobripet.communication.TCTeammateInfo;

import java.util.List;

/**
 * Created by Petr on 7/16/2017.
 * Goal to navigate to rendezvous position
 */
public class Rendezvous extends Goal {

    public Rendezvous(CTFBot bot) {
        super(bot);
    }


    @Override
    public void perform() {
        //at location
        if(bot.getInfo().atLocation(bot.getRendezvousLocation(),300)){
            bot.getBot().getBotName().setInfo("WAITING AT RENDEZVOUS");
        }
        //go to location
        bot.getBot().getBotName().setInfo("GOING TO RENDEZVOUS");
        bot.goTo(bot.getRendezvousLocation());
    }

    @Override
    public double getPriority() {
        if(bot.getCTF().isBotCarryingEnemyFlag()){
            return 0d;
        }
        for (UnrealId botId : bot.getSupportRequests().keySet()) {
            //cannot support myself
            if (botId == bot.getInfo().getId()) {
                continue;
            }
            SupportType requestType = bot.getSupportRequests().get(botId);
            if (requestType == SupportType.GET_OUR_FLAG_RENDEZVOUS) {
                return Priority.RENDEZVOUS_PRIORITY;
            }
        }
        return 0d;
    }

    @Override
    public void abandon() {
        bot.getLog().fine("abandoning Rendezvous");
        return;
    }
}
