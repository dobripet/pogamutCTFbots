package cz.zcu.fav.kiv.dobripet.goals;

import cz.zcu.fav.kiv.dobripet.CTFBot;

public abstract class Goal implements IGoal {

	protected CTFBot bot;

	protected int id;

	public Goal(CTFBot bot) {
		this.bot = bot;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
