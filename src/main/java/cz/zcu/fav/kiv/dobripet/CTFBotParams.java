package cz.zcu.fav.kiv.dobripet;

import cz.cuni.amis.pogamut.ut2004.bot.params.UT2004BotParameters;

/**
 * Adding skill param to UT2004BotParameters
 * 
 * @author dobripet
 */
public class CTFBotParams extends UT2004BotParameters {
	// default skill level
	private int skill = 4;

	public int getSkill() {
		return skill;
	}

	public CTFBotParams setSkill(int skill) {
		this.skill= skill;
		return this;
	}
	
}
