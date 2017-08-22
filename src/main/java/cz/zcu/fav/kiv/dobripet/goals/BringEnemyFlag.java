package cz.zcu.fav.kiv.dobripet.goals;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.utils.Cooldown;
import cz.zcu.fav.kiv.dobripet.*;
import cz.zcu.fav.kiv.dobripet.communication.TCSupportUpdate;
import cz.zcu.fav.kiv.dobripet.communication.TCTeammateInfo;

/**
 * Goal to get enemy flag and bring it home.
 */
public class BringEnemyFlag extends Goal {

	private SupportType previousType;

	//30 second cooldown to do rendezvous attack
	private Cooldown rendezvousAttackCooldown = new Cooldown(30000);

	public BringEnemyFlag(CTFBot bot) {
		super(bot);
	}

	@Override
	public void perform() {
	    Location target = null;
		if (bot.getCTF().isBotCarryingEnemyFlag()) {
			if(bot.getCombo().canPerformCombo() && bot.getInfo().getHealth() < 100){
				bot.getCombo().performDefensive();
			}
			//flag go home, go score
			if(bot.getCTF().isOurFlagHome()){
				target = bot.getCTF().getOurBase().getLocation();
				bot.getBot().getBotName().setInfo("CARRY ENEMY FLAG - GOING HOME");
				bot.getLog().info("GO TO OUR BASE");
			} else {
				target = bot.getHideCarryLocation();
				bot.getBot().getBotName().setInfo("CARRY ENEMY FLAG - GOING TO HIDEOUT");
				bot.getLog().info("GO TO OUR HIDEOUT");
				//already at location, change support type
				if(bot.getInfo().isAtLocation(target, 1000)) {
					//our flag is near enemy base, expecting heavy resistance, call coordinated attack
					if(bot.getPathDistance(bot.getOurFlagLocation(), bot.getCTF().getEnemyBase().getLocation()) < 2000) {
						if (previousType == null || !previousType.equals(SupportType.GET_OUR_FLAG_RENDEZVOUS) && rendezvousAttackCooldown.isCool()) {
							bot.getTCClient().sendToTeamOthers(new TCSupportUpdate(bot.getInfo().getId(), SupportType.GET_OUR_FLAG_RENDEZVOUS, false));
							bot.getLog().fine("SENDING SUPPORT UPDATE: GET_OUR_FLAG_RENDEZVOUS");
							bot.getSupportRequests().put(bot.getInfo().getId(), SupportType.GET_OUR_FLAG_RENDEZVOUS);
							previousType = SupportType.GET_OUR_FLAG_RENDEZVOUS;
						}
						//if bots are ready start attack
						if(rendezvousAttackCooldown.isCool()) {
							int ready = 0;
							for(TCTeammateInfo teammateInfo : bot.getTeammates().values()) {
								if(bot.getPathDistance(teammateInfo.getLocation(), bot.getRendezvousLocation()) < 500) {
									ready++;
								}
							}
							if(ready > bot.getTeammates().size()-1 && ready > 1) {
								bot.getTCClient().sendToTeamOthers(new TCSupportUpdate(bot.getInfo().getId(), SupportType.GET_OUR_FLAG, false));
								bot.getLog().fine("SENDING SUPPORT UPDATE: GET_OUR_FLAG AFTER RENDEZVOUS");
								bot.getSupportRequests().put(bot.getInfo().getId(), SupportType.GET_OUR_FLAG);
								previousType = SupportType.GET_OUR_FLAG;
								rendezvousAttackCooldown.use();
							}
						}
					}else {
						//change support type to get our flag
						if (previousType == null || !previousType.equals(SupportType.GET_OUR_FLAG)) {
							bot.getTCClient().sendToTeamOthers(new TCSupportUpdate(bot.getInfo().getId(), SupportType.GET_OUR_FLAG, false));
							bot.getLog().fine("SENDING SUPPORT UPDATE: GET_OUR_FLAG");
							bot.getSupportRequests().put(bot.getInfo().getId(), SupportType.GET_OUR_FLAG);
							previousType = SupportType.GET_OUR_FLAG;
						}
					}
				}
			}
			//ask for support if not asked yet
			if(previousType == null || !previousType.equals(SupportType.DEFEND_FLAG_CARRY)){
				bot.getTCClient().sendToTeamOthers( new TCSupportUpdate(bot.getInfo().getId(), SupportType.DEFEND_FLAG_CARRY, false));
				bot.getLog().fine("SENDING SUPPORT UPDATE: DEFEND_FLAG_CARRY");
				bot.getSupportRequests().put(bot.getInfo().getId(), SupportType.DEFEND_FLAG_CARRY);
				previousType = SupportType.DEFEND_FLAG_CARRY;
			}
		}else if (bot.getCTF().isEnemyFlagDropped()) {
			bot.goTo(bot.getEnemyFlagLocation());
			bot.getBot().getBotName().setInfo("GOING FOR DROPED ENEMY FLAG");
			bot.getLog().info("GO TO PICK DROPED ENEMY FLAG");
			return;
		} else {
		    target = bot.getEnemyFlagLocation();
			bot.getBot().getBotName().setInfo("GOING FOR ENEMY FLAG");
			bot.getLog().info("GO TO PICK ENEMY FLAG");
		}
        bot.goToCover(target);
        bot.getLog().fine("GO COVER PATH");
        //something bad happened, navigate standard
        if(!bot.isCoverPathNavigating()) {
            bot.goTo(target);
            bot.getLog().info("COVER CHANGED TO STANDARD NAVIGATION");
        }
	}

	@Override
	public double getPriority() {
		if (bot.getCTF().isBotCarryingEnemyFlag()) {
			return Priority.BRING_CARRY_PRIORITY;
		} else if(bot.getCTF().isOurTeamCarryingEnemyFlag()) {
			return 0d;
		} else if(bot.getCTF().isEnemyFlagDropped() &&  bot.getEnemyFlagLocation() != null && Utils.isNearest(bot, bot.getEnemyFlagLocation())){
			return Priority.BRING_PICK_FLAG_PRIORITY;
		} else if (bot.getRole() == Role.ATTACKER){
			return  Priority.BRING_ATTACKER_PRIORITY;
		} else{
			return  Priority.BRING_DEFENDER_PRIORITY;
		}
	}

	@Override
	public void abandon() {
		previousType = null;
		rendezvousAttackCooldown.clear();
		bot.getLog().info("abandoning bringEnemyFlag");
	}
}
