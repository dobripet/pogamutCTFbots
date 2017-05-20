package cz.zcu.fav.kiv.dobripet.goals;

public interface IGoal{
	
	void perform();

	double getPriority();

	void abandon();

	int getId();

	void setId(int id);
}
