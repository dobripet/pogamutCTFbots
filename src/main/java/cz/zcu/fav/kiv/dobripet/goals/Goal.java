package cz.zcu.fav.kiv.dobripet.goals;

import cz.zcu.fav.kiv.dobripet.CTFBot;

public abstract class Goal implements IGoal {

	protected CTFBot bot;

	public Goal(CTFBot bot) {
		this.bot = bot;
	}
}
