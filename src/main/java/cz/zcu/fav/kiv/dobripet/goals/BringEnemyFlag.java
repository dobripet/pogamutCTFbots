package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Priority;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.SupportType;
import cz.zcu.fav.kiv.dobripet.communication.TCSupportUpdate;

public class BringEnemyFlag extends Goal {

	private boolean askedForSupport = false;
	private boolean isAtOurBase = false;
	private Location lastTarget;

	public BringEnemyFlag(CTFBot bot) {
		super(bot);
	}

	@Override
	public void perform() {
	    Location target = null;
		if (bot.getCTF().isBotCarryingEnemyFlag()) {
		    target = bot.getCTF().getOurBase().getLocation();
			bot.getBot().getBotName().setInfo("CARRY ENEMY FLAG");
			bot.getLog().info("GO TO OUR BASE");
			//already at our base but flag is not home, change type, try survive
			if(bot.getInfo().isAtLocation(bot.getCTF().getOurBase(), 1000) && !bot.getCTF().isOurFlagHome()) {
				if (!isAtOurBase) {
					bot.getTCClient().sendToTeamOthers( new TCSupportUpdate(bot.getInfo().getId(), SupportType.GET_OUR_FLAG, false));
					bot.getLog().fine("SENDING SUPPORT UPDATE: GET_OUR_FLAG");
					bot.getSupportRequests().put(bot.getInfo().getId(), SupportType.GET_OUR_FLAG);
					//change support type
					isAtOurBase = true;
				}
			}
			//ask for support if not asked yet
			if(!askedForSupport){
				bot.getTCClient().sendToTeamOthers( new TCSupportUpdate(bot.getInfo().getId(), SupportType.DEFEND_FLAG_CARRY, false));
				bot.getLog().fine("SENDING SUPPORT UPDATE: DEFEND_FLAG_CARRY");
				bot.getSupportRequests().put(bot.getInfo().getId(), SupportType.DEFEND_FLAG_CARRY);
				askedForSupport = true;
			}
		}else{
		    target = bot.getEnemyFlagLocation();
			bot.getBot().getBotName().setInfo("GOING FOR ENEMY FLAG");
			bot.getLog().info("GO TO PICK ENEMY FLAG");
		}
        bot.goToCover(target);
        bot.getLog().fine("GO COVER PATH");
        //something bad happened, navigate standard
        /*if(!bot.isCoverPathNavigating()) {
            bot.goTo(target);
            bot.getLog().info("COVER CHANGED TO STANDARD NAVIGATION");
        }*/
        lastTarget = target;
	}

	@Override
	public double getPriority() {
		if (bot.getCTF().isBotCarryingEnemyFlag()) {
			return Priority.BRING_CARRY_PRIORITY;
		} else if(bot.getCTF().isOurTeamCarryingEnemyFlag()){
		    return 0d;
        } else if (bot.getRole() == Role.ATTACKER){
			return  Priority.BRING_ATTACKER_PRIORITY;
		} else{
			return  Priority.BRING_DEFENDER_PRIORITY;
		}
	}

	@Override
	public void abandon() {
		askedForSupport = false;
		isAtOurBase = false;
		lastTarget = null;
		bot.getLog().info("abandoning bringEnemyFlag");
	}
}
