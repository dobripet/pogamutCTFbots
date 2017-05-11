package cz.zcu.fav.kiv.dobripet.goals;

import java.util.Collections;
import java.util.LinkedList;

import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;

public class GoalManager {
	protected final LinkedList<IGoal> goals = new LinkedList<IGoal>();
	protected IGoal currentGoal = null;
	protected UT2004Bot bot;

	public GoalManager(UT2004Bot bot) {
		this.bot = bot;
	}

	public boolean addGoal(IGoal goal) {
		if (!goals.contains(goal)) {
			goals.add(goal);
			return true;
		} else {
			return false;
		}
	}

	public IGoal executeBestGoal() {

		double maxPriority = Double.MIN_VALUE;
		IGoal next_goal = null;
		for (IGoal goal : goals){
			double priority = goal.getPriority();
			if(maxPriority < priority){
				next_goal = goal;
				maxPriority = priority;
			}
		}
		if (next_goal != currentGoal && currentGoal != null) {
			currentGoal.abandon();
		}
		currentGoal = next_goal;

		bot.getLog().fine(
				String.format(
						"CHOSEN GOAL %.2f: %s",
						maxPriority,
						currentGoal.toString()));
		currentGoal.perform();
		return currentGoal;
	}

	public IGoal getCurrentGoal() {
		return currentGoal;
	}

	public void abandonAllGoals() {
		for (IGoal goal : goals) {
			goal.abandon();
		}
	}

	public void clearAllGoals(){
		goals.clear();
	}
}
