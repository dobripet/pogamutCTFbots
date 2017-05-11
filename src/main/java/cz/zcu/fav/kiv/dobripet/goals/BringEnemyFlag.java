package cz.zcu.fav.kiv.dobripet.goals;

import cz.zcu.fav.kiv.dobripet.CTFBot;
import cz.zcu.fav.kiv.dobripet.Role;
import cz.zcu.fav.kiv.dobripet.SupportType;
import cz.zcu.fav.kiv.dobripet.communication.TCSupportUpdate;

public class BringEnemyFlag extends Goal {


	private final double HOLDING_FLAG_PRIORITY = 70;
	private final double ATTACKER_PRIORITY = 45;
	private final double DEFENDER_PRIORITY = 10;

	private boolean askedForSupport = false;
	private boolean isAtOurBase = false;

	public BringEnemyFlag(CTFBot bot) {
		super(bot);
	}

	@Override
	public void perform() {
		if (bot.getCTF().isBotCarryingEnemyFlag()) {
			bot.goTo(bot.getCTF().getOurBase());
			bot.getBot().getBotName().setInfo("CARRY ENEMY FLAG");
			bot.getLog().info("goTo ourFlagBase");
			//already at our base but flag is not home, change type
			if(bot.getInfo().isAtLocation(bot.getCTF().getOurBase(), 500) && !bot.getCTF().isOurFlagHome()) {
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
			bot.goTo(bot.getEnemyFlagLocation());
			bot.getBot().getBotName().setInfo("GOING FOR ENEMY FLAG");
			bot.getLog().info("goTo pick up enemy flag");
		}
	}

	@Override
	public double getPriority() {
		if (bot.getCTF().isBotCarryingEnemyFlag()) {
			return HOLDING_FLAG_PRIORITY;
		} else if (bot.getRole() == Role.ATTACKER && !bot.getCTF().isOurTeamCarryingEnemyFlag()){
			return ATTACKER_PRIORITY;
		} else{
			return DEFENDER_PRIORITY;
		}
	}

	@Override
	public boolean hasFailed() {
		return false;
	}

	@Override
	public boolean hasFinished() {
		return false;
	}

	@Override
	public void abandon() {
		askedForSupport = false;
		isAtOurBase = false;
		bot.getLog().info("abandoning bringEnemyFlag");
	}
}
