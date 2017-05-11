package cz.zcu.fav.kiv.dobripet.goals;

public interface IGoal{
	
	void perform();

	double getPriority();

	boolean hasFailed();

	boolean hasFinished();

	void abandon();
	
}
