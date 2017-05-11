package cz.zcu.fav.kiv.dobripet;

import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pogamut.base.agent.impl.AgentId;
import cz.cuni.amis.pogamut.base.agent.module.comm.PogamutJVMComm;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.PathExecutorState;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassEventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.object.event.WorldObjectUpdatedEvent;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensomotoric.Weapon;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.NavigationState;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathExecutorStuckState;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.pathfollowing.NavMeshNavigation;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.pathfollowing.UT2004AcceleratedPathExecutor;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.AccUT2004DistanceStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.AccUT2004PositionStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.AccUT2004TimeStuckDetector;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004BotModuleController;
import cz.cuni.amis.pogamut.ut2004.bot.params.UT2004BotParameters;
import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.*;
import cz.cuni.amis.pogamut.ut2004.teamcomm.bot.UT2004BotTCController;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.Heatup;
import cz.cuni.amis.utils.Tuple2;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import cz.zcu.fav.kiv.dobripet.communication.*;
import cz.zcu.fav.kiv.dobripet.goals.*;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Example of Simple Pogamut bot, that randomly walks around the map searching
 * for preys shooting at everything that is in its way.
 * 
 * @author Rudolf Kadlec aka ik
 * @author Jimmy, dobripet
 */
@AgentScoped
public class CTFBot extends UT2004BotTCController<UT2004Bot> {

    /*public void draw(Color color, Location loc) {
        draw.clearAll();
        draw.drawLine(color, info.getLocation(), loc);
    }*/

	@Override
	public void prepareBot(UT2004Bot bot) {
		bot.getLogger().setLevel(Level.FINE);
		bot.getLogger().addDefaultFileHandler(new File("CTFBot"+botsInitialized+".txt"));
	}
    // base name of bot
    private String botName;
    // team
    private static int team;
    // status info cooldown
	private final Cooldown statusCD = new Cooldown(1500);
	// target heatup
	private final Heatup targetHU = new Heatup(5000);
	// goal manager
	private GoalManager goalManager = null;
	// number of bots in team
	private static int TEAM_SIZE;
	// bots initialized
    private static int botsInitialized = 0;
    // bot role
	private Role role;
	// pick weapon or ammo goal
	private PickWeaponOrAmmo pickWeaponOrAmmoGoal;
	// pick shield
	private PickShield pickShieldGoal;
	// pick health
	private PickHealth pickHealthGoal;
	// pick adrenaline
	private PickAdrenaline pickAdrenalineGoal;
    // bring enemy flag
    private DefendOurFlag defendOurFlagGoal;
	/*// bring enemy flag
    private BringEnemyFlag bringEnemyFlagGoal;
    // support
    private Support supportGoal;
    // sniper*/
    private Snipe snipeGoal;
	// current navigation target item
	private Item targetItem;
	// currently hunted player
	private Player enemy;

	private Map<UnrealId, SupportType> supportRequests;
    private Map<UnrealId, TCTeammateInfo> teammates;
    private Map<UnrealId, Tuple2<TCEnemyInfo, Cooldown>> enemies;

	// last known enemy flag location
	private Location enemyFlagLocation;
	// last known our flag location
	private Location ourFlagLocation;
	// if flag was not at last known position
    private boolean flagUnknown = false;

	//helper to determine if bot holded flag last logic
	private boolean isCarry;
    private boolean sniper = false;
    // if bot is camping at flag
    private boolean camper = false;
    private boolean supporting;
    //target of current path
    private Location pathTarget;

	//count of stucks
	private int stuckCount;
	private final int STUCK_COUNT_LIMIT = 50;

	// accelerated path executor
	private UT2004AcceleratedPathExecutor pathExecutor;
	// removes unwalkable paths
	private UT2004PathAutoFixer autoFixer;

	// taboo list to forbid items
	private TabooSet<Item> tabooItems = null;

	@Override
	public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
	    //set base name
	    this.botName = bot.getName();
		// use navmesh
		if (navMeshModule.isInitialized()) {
			navigation = new NavMeshNavigation(bot, info, move, navMeshModule);
			//drawmesh
			//navMeshModule.getNavMeshDraw().draw(true, true);
		}
		tabooItems = new TabooSet<Item>(bot);

		pathExecutor = ((UT2004AcceleratedPathExecutor) navigation.getPathExecutor());
		pathExecutor.removeAllStuckDetectors();

		pathExecutor.addStuckDetector(new AccUT2004TimeStuckDetector(bot, 3000, 10000));
		pathExecutor.addStuckDetector(new AccUT2004PositionStuckDetector(bot));
		pathExecutor.addStuckDetector(new AccUT2004DistanceStuckDetector(bot));
		// auto-removes wrong navigation links between navpoints
		autoFixer = new UT2004PathAutoFixer(bot, pathExecutor, fwMap, aStar, navBuilder);

		/*
		navigation.addStrongNavigationListener(
				new FlagListener<NavigationState>() {
					@Override
					public void flagChanged(NavigationState changedValue) {
						navigationStateChanged(changedValue);
					}
				}
		);
*/
		//listeners
		/*
		navigation.addStrongNavigationListener(
				new FlagListener<NavigationState>() {

					@Override
					public void flagChanged(NavigationState changedValue) {
						switch (changedValue) {
							case PATH_COMPUTATION_FAILED:
								//pathComputationErrors++;
								if (navigation.getCurrentPathDirect() == null || navigation.getCurrentPathDirect().isEmpty()) {
									if (targetItem != null) {
										if (tabooItems.contains(targetItem)) {
											log.warning("Item already in tabooItems while being navigation target " + targetItem);
										}
										tabooItems.add(targetItem, 60);
										log.warning("Added to tabooItems " + targetItem);
									}

								}
								if (targetNavPoint == null) {
									targetNavPoint = navigation.getCurrentTargetNavPoint();
								} else if (targetNavPoint.equals(ctf.getOurBase()) || targetNavPoint.equals(ctf.getEnemyBase())) {
									autoFixer.addRemovedLinks();
								} else if (pathComputationErrors >= MAX_PATHFINDING_ERRORS) {
									pathComputationErrors = 0;
									autoFixer.addRemovedLinks();
								}
								break;
						}
					}
				}
		);*/
		// IMPORTANT
		// adds a listener to the path executor for its state changes, it will allow you to
		// react on stuff like "PATH TARGET REACHED" or "BOT STUCK"
		pathExecutor.getState().addStrongListener(new FlagListener<IPathExecutorState>() {
			@Override
			public void flagChanged(IPathExecutorState changedValue) {
				pathExecutorStateChange(changedValue.getState());
			}
		});


		//setup smart shooting
		// general preferences, probably unused cuz of range prefs
		weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.SHIELD_GUN, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);

		//detailed range weapon pref specification
		// < 1m
        this.getWeaponPrefs().newPrefsRange(100)
                .add(UT2004ItemType.SHIELD_GUN, true)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.LINK_GUN, false)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.BIO_RIFLE, true)
                .add(UT2004ItemType.SHIELD_GUN, false);
        // < 1.5m
		this.getWeaponPrefs().newPrefsRange(150)
				.add(UT2004ItemType.FLAK_CANNON, true)
				.add(UT2004ItemType.LINK_GUN, false)
				.add(UT2004ItemType.MINIGUN, true)
				.add(UT2004ItemType.BIO_RIFLE, true)
				.add(UT2004ItemType.SHIELD_GUN, false);

		// < 5m
		this.getWeaponPrefs().newPrefsRange(500)
				.add(UT2004ItemType.FLAK_CANNON, true)
				.add(UT2004ItemType.LINK_GUN, false)
				.add(UT2004ItemType.MINIGUN, true)
				.add(UT2004ItemType.ROCKET_LAUNCHER, true)
				.add(UT2004ItemType.LIGHTNING_GUN, true)
				.add(UT2004ItemType.BIO_RIFLE, true)
				.add(UT2004ItemType.SHIELD_GUN, false);

		// < 10m
		this.getWeaponPrefs().newPrefsRange(1000)
				.add(UT2004ItemType.MINIGUN, true)
				.add(UT2004ItemType.LINK_GUN, true)
				.add(UT2004ItemType.FLAK_CANNON, true)
				.add(UT2004ItemType.SHOCK_RIFLE, true)
				.add(UT2004ItemType.ROCKET_LAUNCHER, true)
				.add(UT2004ItemType.LIGHTNING_GUN, true)
				.add(UT2004ItemType.SHIELD_GUN, false);

		// < 25m
		this.getWeaponPrefs().newPrefsRange(2500)
				.add(UT2004ItemType.SHOCK_RIFLE, true)
				.add(UT2004ItemType.LIGHTNING_GUN, true)
				.add(UT2004ItemType.MINIGUN, false)
				.add(UT2004ItemType.LINK_GUN, true)
				.add(UT2004ItemType.ROCKET_LAUNCHER, true)
				.add(UT2004ItemType.SHIELD_GUN, false);

		// rest
		this.getWeaponPrefs().newPrefsRange(100000)
				.add(UT2004ItemType.LIGHTNING_GUN, true)
				.add(UT2004ItemType.SHOCK_RIFLE, true)
				.add(UT2004ItemType.MINIGUN, false)
				.add(UT2004ItemType.ROCKET_LAUNCHER, true)
				.add(UT2004ItemType.SHIELD_GUN, false);

		// create goal manager
		goalManager = new GoalManager(this.bot);
		//select starting role
		if(botsInitialized % 2 == 0){
		    role = Role.ATTACKER;
        } else{
		    role = Role.DEFENDER;
        }
        botsInitialized++;

		goalManager.addGoal(pickWeaponOrAmmoGoal = new PickWeaponOrAmmo(this));
		goalManager.addGoal(pickShieldGoal = new PickShield(this));
		goalManager.addGoal(pickHealthGoal = new PickHealth(this));
        goalManager.addGoal(pickAdrenalineGoal = new PickAdrenaline(this));
		goalManager.addGoal(new BringEnemyFlag(this));
        goalManager.addGoal(defendOurFlagGoal = new DefendOurFlag(this));
        goalManager.addGoal(new Support(this));
		List<Item> pickWeaponOrAmmo = new ArrayList<Item>();
		List<Item> pickShield = new ArrayList<Item>();
		List<Item> pickHealth = new ArrayList<Item>();
		List<Item> pickAdrenaline = new ArrayList<Item>();

		pickHealth.addAll(items.getAllItems(UT2004ItemType.UT2004Group.HEALTH).values());
		pickHealth.addAll(items.getAllItems(UT2004ItemType.UT2004Group.MINI_HEALTH).values());
		pickHealth.addAll(items.getAllItems(UT2004ItemType.UT2004Group.SUPER_HEALTH).values());
		pickAdrenaline.addAll(items.getAllItems(UT2004ItemType.UT2004Group.ADRENALINE).values());
		if(navBuilder.isMapName("CTF-Citadel")){

            //navBuilder.removeEdgesBetween("CTF-Citadel.PathNode75", "CTF-Citadel.JumpSpot26");
            //draw.drawLine(Color.cyan, navPoints.getNavPoint("CTF-Citadel.PathNode75").getLocation(), navPoints.getNavPoint("CTF-Citadel.JumpSpot26").getLocation());
            //draw.drawLine(Color.cyan, navPoints.getNavPoint("CTF-Citadel.PathNode64").getLocation(), navPoints.getNavPoint("CTF-Citadel.jumpspot9").getLocation());
			log.info("LOADING CUSTOM SETUP FOR CTF-Citadel");
            navBuilder.removeEdgesBetween("CTF-Citadel.PathNode99", "CTF-Citadel.JumpSpot27");
            navBuilder.removeEdgesBetween("CTF-Citadel.PathNode36", "CTF-Citadel.JumpSpot5");
			navBuilder.removeEdge("CTF-Citadel.PathNode26", "CTF-Citadel.PathNode23");
            navBuilder.removeEdgesBetween("CTF-Citadel.InventorySpot179", "CTF-Citadel.InventorySpot193");
			pickHealth.removeAll(items.getAllItems(UT2004ItemType.UT2004Group.SUPER_HEALTH).values());
			System.out.println("pozor ozor " + pickHealth.size());
			tabooItems.add(items.getItem("CTF-Citadel.InventorySpot232"));
			tabooItems.add(items.getItem("CTF-Citadel.InventorySpot233"));
			System.out.println(
					tabooItems.getTabooTime(items.getItem("CTF-Citadel.InventorySpot222")));
			pickWeaponOrAmmo.addAll(items.getAllItems(UT2004ItemType.UT2004Group.MINIGUN).values());
			pickWeaponOrAmmo.addAll(items.getAllItems(UT2004ItemType.UT2004Group.LIGHTNING_GUN).values());
			pickWeaponOrAmmo.addAll(items.getAllItems(UT2004ItemType.UT2004Group.ROCKET_LAUNCHER).values());
			pickWeaponOrAmmo.addAll(items.getAllItems(UT2004ItemType.UT2004Group.SHOCK_RIFLE).values());
			pickWeaponOrAmmo.addAll(items.getAllItems(UT2004ItemType.UT2004Group.BIO_RIFLE).values());
            List<Location> snipingSpots = new ArrayList<Location>();
            List<Location> snipingFocusSpots = new ArrayList<Location>();
            List<Location> defendSpots = new ArrayList<Location>();
            List<Location> defendFocusSpots = new ArrayList<Location>();
			if(team == 0){
                snipingSpots.add(new Location(-127.03, 2455.29, 457.90) );
                snipingSpots.add(new Location(-285.89, 2433.07, 443.90) );
                snipingFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot175").getLocation());
                snipingFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot231").getLocation());
                snipingFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot159").getLocation());
                defendSpots.add(navPoints.getNavPoint("CTF-Citadel.PlayerStart0").getLocation());
                defendSpots.add(navPoints.getNavPoint("CTF-Citadel.PlayerStart2").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.PathNode16").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.PathNode104").getLocation());

            }else{
                snipingSpots.add(navPoints.getNavPoint("CTF-Citadel.AIMarker25").getLocation());
                snipingSpots.add(navPoints.getNavPoint("CTF-Citadel.AIMarker35").getLocation());
                snipingFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot172").getLocation());
                snipingFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot230").getLocation());
                snipingFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot158").getLocation());
                defendSpots.add(new Location(-90.35, -4937.48, -1822.15));
                defendSpots.add(new Location(-518.84, -4921.11, -1822.15));
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.PathNode37").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Citadel.PathNode34").getLocation());
            }
            goalManager.addGoal(snipeGoal = new Snipe(this));
			snipeGoal.setSnipingSpots(snipingSpots);
			snipeGoal.setSnipingFocusSpots(snipingFocusSpots);
			defendOurFlagGoal.setDefendSpots(defendSpots);
			defendOurFlagGoal.setDefendFocusSpots(defendFocusSpots);

		}else {
			//pick all weapons and ammo
			pickWeaponOrAmmo.addAll(items.getAllItems(ItemType.Category.WEAPON).values());
			pickWeaponOrAmmo.addAll(items.getAllItems(ItemType.Category.AMMO).values());
		}
		pickShield.addAll(items.getAllItems(UT2004ItemType.UT2004Group.SMALL_ARMOR).values());
		pickShield.addAll(items.getAllItems(UT2004ItemType.UT2004Group.SUPER_ARMOR).values());

		for(Item i : pickHealth){
			System.out.println(i);
		}
		pickWeaponOrAmmoGoal.setItemsToPickUp(pickWeaponOrAmmo);
		pickShieldGoal.setItemsToPickUp(pickShield);
		pickHealthGoal.setItemsToPickUp(pickHealth);
		pickAdrenalineGoal.setItemsToPickUp(pickAdrenaline);
        supportRequests = new HashMap<UnrealId, SupportType>();
        teammates = new HashMap<UnrealId, TCTeammateInfo>();
        enemies = new HashMap<UnrealId, Tuple2<TCEnemyInfo, Cooldown>>();
	}


	/**
	 * Path executor has changed its state (note that {@link UT2004BotModuleController#getPathExecutor()} is internally used by
	 * {@link UT2004BotModuleController#getNavigation()} as well!).
	 *
	 * @param state
	 */
	private void pathExecutorStateChange(PathExecutorState state) {
		System.out.println("mrdka z krtka" + state);
		switch(state) {
			case PATH_COMPUTATION_FAILED:
				// if navigating to item taboo it
				if (targetItem != null) {
					log.severe("PATH FAILED WITH" + targetItem);
					tabooItems.add(targetItem, 60);
				}
				break;

			case STUCK:
				// if navigating to item taboo it
				if (targetItem != null) {
					log.severe("STUCK WTIH " + targetItem);
					tabooItems.add(targetItem, 20);
				}
				break;

			case TARGET_REACHED:
				//item is not spawned, do not wait and notify others
				if (targetItem != null && pathTarget != null) {
					tabooItems.add(targetItem, items.getItemRespawnTime(targetItem));
					log.info("NOT SPAWNED " + targetItem);
					if(targetItem.getId() == null){
					    log.severe("ID NULL TARGETREACHED");
					    System.exit(0);
                    }
					tcClient.sendToTeamOthers(new TCItemPickedUp(targetItem.getId()));
				}
				break;
/*
			case STOPPED:
				// if navigating to item taboo it
				if (targetItem != null) {
					log.severe("STOPPED WTIH " + targetItem);
					tabooItems.add(targetItem, 60);
				}
				break;*/
		}
	}

	/**
	 * Taboo picked items and notify team
	 */
	@EventListener(eventClass = ItemPickedUp.class)
	private void itemPickedUp(ItemPickedUp event) {
		Item item = items.getItem(event.getId());
		if(item != null ){

            if(item.getId() == null){
                log.severe("ID NULL ITEMPICKUP");
                System.exit(0);
            }
			tabooItems.add(item, items.getItemRespawnTime(item));
            tcClient.sendToTeamOthers(new TCItemPickedUp(item.getId()));
		}else {
			log.severe("PICKUP EVENT ITEM NULL");
		}
	}

    /**
     * Process picked up items from teammate
     * @param msg
     */
    @EventListener(eventClass = TCItemPickedUp.class)
    private void processTCItemPickedUp(TCItemPickedUp msg) {
	    Item item = items.getItem(msg.getId());
	    if(item == null){
            log.severe( "TEAMMATE PICKED UP NULL");
	        return;
        }
        log.info( "TEAMMATE PICKED UP " +item);
	    tabooItems.add(item, items.getItemRespawnTime(item));
    }

	/**
	 * Process info update of teammate
	 * @param msg
	 */
	@EventListener(eventClass = TCTeammateInfo.class)
	public void processTCTeammateInfo(TCTeammateInfo msg) {
        //update teammate info
	    teammates.put(msg.getBotID(),msg);
		//not pickup same item as other bot
		if(msg.getTargetItemId()!= null){
			tabooItems.add(items.getItem(msg.getTargetItemId()),1.5);
		}
		log.severe( "TEAMMATE INFO UPDATE: " + msg);
	}

    /**
     * Process info update of enemy
     * @param msg
     */
    @EventListener(eventClass = TCEnemyInfo.class)
    public void processTCEnemyInfo(TCEnemyInfo msg) {
        //update enemy info
        enemies.put(msg.getBotID(), new Tuple2<TCEnemyInfo, Cooldown>(msg, new Cooldown(1000)));
        //start cooldown
        enemies.get(msg.getBotID()).getSecond().use();
        log.severe( "ENEMY INFO UPDATE: " + msg);
    }


    /**
     * Choose priority enemy, browse visible enemies, so update status to teammates
     * @return chosen Player and if he is carrying our flag
     */
	private Player choosePrimaryEnemy(){
        Player target = null;
        double maxPriority = Double.MIN_VALUE;
        for(Player player : players.getVisibleEnemies().values()){
            // always shoot on flag holder
            if(this.ctf.isOurFlagHeld() && this.ctf.getOurFlag().isVisible() && this.ctf.getOurFlag().getHolder() == player.getId()) {
                enemyUpdate(player, true);
                target = player;
                maxPriority = Double.MAX_VALUE;
            } else {
                enemyUpdate(player, false);
                double priority = 0d;
                //if he has weapon better then assault rifle
                if (!player.getWeapon().equals("AssaultRifle")) {
                    priority = 5d;
                }
                priority += 5000d / (info.getLocation().getDistance(player.getLocation()));
                if (priority > maxPriority) {
                    maxPriority = priority;
                    target = player;
                }
            }
        }
        return target;
    }

    /**
     * Updates enemies info to state of current vision and notify team if time elapsed
     * @param player seen player
     * @param carry is he carrying our flag
     */
    private void enemyUpdate(Player player, boolean carry){
	    //first time seen player, message cooldown 1sec
	    if(enemies.get(player.getId()) == null){
	        // null because we want to update and notify team
	        enemies.put(player.getId(), new Tuple2<TCEnemyInfo, Cooldown>(new TCEnemyInfo(player.getId(), null, carry), new Cooldown(1000)));
        }
        Location old = enemies.get(player.getId()).getFirst().getLocation();
        if(player.getLocation() != old) {
            //send message if time elapsed
            if (enemies.get(player.getId()).getSecond().isCool()) {
                tcClient.sendToTeamOthers(new TCEnemyInfo(player.getId(), player.getLocation(), carry));
                enemies.get(player.getId()).getSecond().use();
            }
        }
        //update locals
        enemies.get(player.getId()).getFirst().setLocation(player.getLocation());
        enemies.get(player.getId()).getFirst().setCarry(carry);
    }

	private void updateFight() {
        Player primaryEnemy = choosePrimaryEnemy();
        //no visible target
        if(primaryEnemy == null){
                //hot target with our flag, enemy flag not carried by bot
                if(enemy != null && targetHU.isHot() && enemies.get(enemy.getId()).getFirst().isCarry() && !isCarry){
                    //follow him and hunt
                    goTo(enemies.get(enemy.getId()).getFirst().getLocation());
                }else{
                    enemy = null;
                }
                stopShooting();
        }else {
            // carry lost vision, try to follow and shoot what sees
            if(primaryEnemy != enemy && enemy != null && enemies.get(enemy.getId()).getFirst().isCarry() && targetHU.isHot() && !isCarry){
                goTo(enemies.get(enemy.getId()).getFirst().getLocation());
                shoot(primaryEnemy);
            }else {
                targetHU.heat();
                enemy = primaryEnemy;
                //aggressive attack
                if(supporting){
                    followPlayer(enemy);
                }
                shoot(enemy);
            }
        }
	}
	private void shoot(Player target){
        if (target!= null && target.isVisible()) {
            double distance = info.getLocation().getDistance(target.getLocation());
            // don't waste bullets on long range
            if(distance > 5000 && !sniper) {
                stopShooting();
                return;
            }
            // don't shoot long with no long range weapon
            if(distance > 3000 && !hasSniperGun()) {
                stopShooting();
                return;
            }
            navigation.setFocus(target);
            shoot.shoot(weaponPrefs, target);
        } else {
            stopShooting();
        }
    }

	private void stopShooting(){
		navigation.setFocus(null);
		shoot.stopShooting();
	}

    @EventListener(eventClass = PlayerKilled.class)
    private void playerKilled(PlayerKilled event) {
	    //if player got killed clear heat
        if(enemy != null && event.getId() == enemy.getId()) {
            targetHU.clear();
            enemy = null;
        }
/*
        if(this.ctf.getOurFlag().isVisible() && !this.ctf.isOurFlagHome()) {
            this.navigation.navigate(this.ctf.getOurFlag().getLocation());
        }
*/
    }
	/**
	 * Updates flags location for current vision and state, notify team
	 */
	private void updateFlagsLocation(){
		TCUpdateFlagLocation msg = null;
		//enemyflag
		if(ctf.isEnemyFlagHome()) {
			//no need to send message, next logic every bot will know and update their location
			enemyFlagLocation = ctf.getEnemyBase().getLocation();
		} else if(ctf.isBotCarryingEnemyFlag()) {
			if(enemyFlagLocation != info.getLocation()) {
				enemyFlagLocation = info.getLocation();
				// notify all with current flag location
				msg = new TCUpdateFlagLocation(enemyFlagLocation, null);
			}
		} else if(ctf.getEnemyFlag().isVisible()) {
			// notify all if location is different and flag is just laying around
			if(enemyFlagLocation != ctf.getEnemyFlag().getLocation() && !ctf.isOurTeamCarryingEnemyFlag()){
				enemyFlagLocation = info.getLocation();
				msg = new TCUpdateFlagLocation(enemyFlagLocation, null);
			}
		}

		//our flag
		if(ctf.isOurFlagHome()) {
			//no need to send message, next logic every bot will know and update their location
			ourFlagLocation = ctf.getOurBase().getLocation();
			flagUnknown = false;
		} else if(this.ctf.getOurFlag().isVisible()) {
		    flagUnknown = false;
			if(ourFlagLocation != ctf.getOurFlag().getLocation()) {
				ourFlagLocation = ctf.getOurFlag().getLocation();
				// notify all with current flag location
				if( msg == null){
					msg = new TCUpdateFlagLocation(null, ourFlagLocation);
				}
				else{
					msg.setOurFlagLocation(ourFlagLocation);
				}
			}
		}
		//send message if something new happened
		if(msg != null){
			tcClient.sendToTeamOthers(msg);
			log.fine("FLAG LOCATION UPDATED SENT " + enemyFlagLocation + " " + ourFlagLocation);
		}
	}

	/**
	 * Process new flag locations from teammates
	 * @param msg
	 */
	@EventListener(eventClass = TCUpdateFlagLocation.class)
	private void processTCUpdateFlagLocation(TCUpdateFlagLocation msg){
		if(msg.getEnemyFlagLocation() != null){
			enemyFlagLocation = msg.getEnemyFlagLocation();
		}
		if(msg.getOurFlagLocation() != null){
			ourFlagLocation = msg.getOurFlagLocation();
			flagUnknown = false;
		}
		log.fine("FLAG LOCATION UPDATED " + enemyFlagLocation + " " + ourFlagLocation);
	}

    /**
     * Process support request from teammate
     * @param msg
     */
	@EventListener(eventClass = TCSupportUpdate.class)
	private void processSupportRequest(TCSupportUpdate msg){
		//revoke support request
		if(msg.isRevoking()){
			if(supportRequests.containsKey(msg.getBotId())) {
				supportRequests.remove(msg.getBotId());
			}
		}else {
            supportRequests.put(msg.getBotId(), msg.getSupportType());
        }
        log.fine("SUPPORT UPDATED " + msg.getBotId() + " " + msg.getSupportType() + " " + msg.isRevoking() );
	}

	private void navigationStateChanged(NavigationState changedValue) {
		body.getCommunication().sendGlobalTextMessage(changedValue.toString());
	}



	private void pathExecutorStuck(UT2004PathExecutorStuckState state) {
/*		body.getCommunication().sendGlobalTextMessage(state.getStuckDetector().getClass().getSimpleName());
		if (state.getLink() != null) {
			NavPointNeighbourLink link = state.getLink();
			body.getCommunication().sendGlobalTextMessage(link.getFromNavPoint().getId().getStringId() + " -> " + link.getToNavPoint().getId().getStringId());
		}*/
		// MIGHT BE USEFUL?
		// TODO: uncomment
		//System.exit(1);
	}



	/**
	 * Returns parameters of the bot.
	 * @return
	 */
	public CTFBotParams getParams() {
		if (!(bot.getParams() instanceof CTFBotParams)) return null;
		return (CTFBotParams)bot.getParams();
	}
	
	public Location getPathTarget() {
		return pathTarget;
	}

	protected boolean firstLogic = true;

	/**
	 * Bot's preparation - called before the bot is connected to GB2004 and
	 * launched into UT2004.
	 */
	/*
	@Override
	public void prepareBot(UT2004Bot bot) {
		tabooItems = new TabooSet<Item>(bot);

	    autoFixer = new UT2004PathAutoFixer(bot, navigation.getPathExecutor(), fwMap, aStar, navBuilder); // auto-removes wrong navigation links between navpoints

		// listeners
		navigation.getPathExecutor().getState().addListener(
				new FlagListener<IPathExecutorState>() {
					@Override
					public void flagChanged(IPathExecutorState changedValue) {
						switch (changedValue.getState()) {
							case STUCK:
								Item item = getPickItemsGoal.getItem();
								if (item != null && pathTarget != null
										&& item.getLocation()
												.equals(pathTarget, 10d)) {
									tabooItems.add(item, 10);
								}
								reset();
								break;

							case TARGET_REACHED:
								reset();
								break;
						}
					}
				});

		// DEFINE WEAPON PREFERENCES
		weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.LIGHTNING_GUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.SHOCK_RIFLE, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.ROCKET_LAUNCHER, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.ASSAULT_RIFLE, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, false);
		weaponPrefs.addGeneralPref(UT2004ItemType.FLAK_CANNON, true);
		weaponPrefs.addGeneralPref(UT2004ItemType.BIO_RIFLE, true);

		goalManager = new GoalManager(this.bot);

		goalManager.addGoal(new BringEnemyFlag(this));
		goalManager.addGoal(new GetHealth(this));
		goalManager.addGoal(getPickItemsGoal = new PickWeaponOrAmmo(this));
		goalManager.addGoal(new CloseInOnEnemy(this));

	}
*/

	/**
	 * Modify bot for given starting params
	 * @return
	 */
	@Override
	public Initialize getInitializeCommand() {
		if (getParams() == null) {
			return new Initialize();
		} else {
			return new Initialize().setDesiredSkill(getParams().getSkill()).setTeam(getParams().getTeam());
		}
	}
	
	@Override
	public void botFirstSpawn(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init, Self self) {
		//join to right channel
		PogamutJVMComm.getInstance().registerAgent(bot, self.getTeam());
		//log.setLevel(Level.WARNING);
		//navigation.setLogLevel(Level.FINEST);
	}

	@Override
	public void botShutdown() {
		PogamutJVMComm.getInstance().unregisterAgent(bot);
	}

	public void pickItem(Item item){
	    targetItem = item;
        if (targetItem == null) {
            bot.getLog().severe("PICKITEM IS NULL");
            navigation.stopNavigation();
            return;
        }
        if(info.atLocation(targetItem)) {
            bot.getLog().info("PICKITEM IS NOT THERE");
            tabooItems.add(targetItem, items.getItemRespawnTime(targetItem));
            navigation.getRunStraight();
            targetItem = null;
            return;
        }
        bot.getLog().fine("PICKITEM GOING FOR " +targetItem);
        goTo(targetItem);
    }

	public void goTo(ILocated target) {
		if (target == null) {
			log.severe("GOTO TARGET NULL");
			navigation.stopNavigation();
			return;
		}
		log.info("GOTO " +target.getLocation() + " " +target);
		pathTarget = target.getLocation();
        if(pathTarget == null){
            log.severe("GOTO TARGET LOCATION NULL");
            navigation.stopNavigation();
        }
		IPathFuture<ILocated> path = navMeshModule.getNavMesh().computePath(bot.getLocation(), target.getLocation());
		navigation.navigate(target);
	}

	public void followPlayer(Player player){
	    if(player == null || player.getLocation() == null){
	        log.severe("FOLLOW PLAYER NULL");
	        return;
        }
	    navigation.navigate(player);
    }

	/**
	 * Resets the state of the Hunter.
	 */
	protected void reset() {
		notMoving = 0;
		enemy = null;
		navigation.stopNavigation();
	}

	/**
	 * Global anti-stuck mechanism. When this counter reaches a certain
	 * constant, the bot's mind gets a {@link CTFBot#reset()}.
	 */
	protected int notMoving = 0;

	/**
	 * Main method that controls the bot - makes decisions what to do next. It
	 * is called iteratively by Pogamut engine every time a synchronous batch
	 * from the environment is received. This is usually 4 times per second - it
	 * is affected by visionTime variable, that can be adjusted in GameBots ini
	 * file in UT2004/System folder.
	 *
	 */
	@Override
	public void logic() {
		log.warning("jsem bot " + bot.getName() + " a v tymu nas je " + TEAM_SIZE);
		log.severe("navesh je " +navMeshModule.isInitialized());

		log.severe(info.getName() + " CURRENT ROLE " +role);
        //flags processing
        updateFlagsLocation();
        log.info("CARRY " +isCarry + " " +ctf.isBotCarryingEnemyFlag());
        if(!ctf.isBotCarryingEnemyFlag() && isCarry){
            //cancel support request, not carry flag anymore
            tcClient.sendToTeamOthers(new TCSupportUpdate(info.getId(), null, true));
            bot.getLog().fine("SENDING SUPPORT UPDATE OFF");
        }
        isCarry = ctf.isBotCarryingEnemyFlag();
        sniper = false;
        // update location of visible friends
        for(Player player : players.getVisibleFriends().values()){
            if(teammates.get(player.getId())==null){
                teammates.put(player.getId(), new TCTeammateInfo(player.getId(),null,player.getLocation(), null, null, false, false));
            }
            teammates.get(player.getId()).setLocation(player.getLocation());
        }

		goalManager.executeBestGoal();
        /*if(camping){
            navigation.stopNavigation();
        }*/

        //updatefight
        updateFight();

		//notify others on cooldown to prevent spamm
		if(statusCD.isCool()) {
			UnrealId targetItemId = null;
			if (targetItem != null) {
				targetItemId = targetItem.getId();
			}
			tcClient.sendToTeamOthers(new TCTeammateInfo(info.getId(), role, info.getLocation(), targetItemId, botName, sniper, camper));
			statusCD.use();
		}




		/*navigation.navigate(ctf.getOurBase());
		if(navigation.isNavigating()){
			System.out.println("wtf");
		}

		for( UnrealId id : items.getAllItems(ItemType.Category.WEAPON).keySet()){
			log.severe("ITEM: " +  items.getAllItems(ItemType.Category.WEAPON).get(id) + " " +
			fwMap.getDistance(ctf.getOurBase(), items.getAllItems(ItemType.Category.WEAPON).get(id).getNavPoint()));
		}
		//log.severe("NEAREST ITEM: " + navigation.getPathNearestItem(ItemType.Category.WEAPON).getLocation());
*/
		log.info("OUR FLAG:                      " + ctf.getOurFlag());
		log.info("OUR BASE:                      " + ctf.getOurBase());
		log.info("ENEMY FLAG LOCATION:           " + enemyFlagLocation);
		log.info("OUR FLAG LOCATION:             " + ourFlagLocation);
		log.info("CAN OUR TEAM POSSIBLY SCORE:   " + ctf.canOurTeamPossiblyScore());
		log.info("CAN OUR TEAM SCORE:            " + ctf.canOurTeamScore());
		log.info("CAN BOT SCORE:                 " + ctf.canBotScore());
		log.info("ENEMY FLAG:                    " + ctf.getEnemyFlag());
		log.info("ENEMY BASE:                    " + ctf.getEnemyBase());
		log.info("CAN ENEMY TEAM POSSIBLY SCORE: " + ctf.canEnemyTeamPossiblyScore());
		log.info("CAN ENEMY TEAM SCORE:          " + ctf.canEnemyTeamScore());
	}

	//camping stuff
    public boolean isAnybodyCamper(){
        for(TCTeammateInfo info : teammates.values()){
            if(info.isCamper()){
                return true;
            }
        }
        return false;
    }

	// sniping stuff
    public boolean isAnybodySniper() {
        for(TCTeammateInfo info : teammates.values()){
            if(info.isSniper()){
                return true;
            }
        }
        return false;
    }

    public boolean hasSniperGun() {
        if(weaponry.hasLoadedWeapon(UT2004ItemType.LIGHTNING_GUN) || weaponry.hasLoadedWeapon(UT2004ItemType.SHOCK_RIFLE)){
            return true;
        }
        return false;
    }

    /**
	 * Rocket dodging
	 * @param event
	 */
	@ObjectClassEventListener(objectClass = IncomingProjectile.class, eventClass = WorldObjectUpdatedEvent.class)
	protected void incomingProjectile(WorldObjectUpdatedEvent<IncomingProjectile> event) {
		if(event.getObject().getLocation().getDistance(info.getLocation()) < 750){
			if(random.nextDouble() < 0.5) {
				move.dodgeLeft(info.getLocation().add(info.getRotation().toLocation()), true);
			} else{
				move.dodgeRight(info.getLocation().add(info.getRotation().toLocation()), true);
			}
		}
	}


	public TabooSet<Item> getTaboo() {
		return tabooItems;
	}

    /**
     * Team comm
     * @param tcMessage
     * @return
    public String toString(TCMessage tcMessage) {
        StringBuffer sb = new StringBuffer();
        sb.append(tcMessage.getTarget());
        switch(tcMessage.getTarget()) {
            case CHANNEL:
                sb.append("[");
                sb.append(tcMessage.getChannelId());
                sb.append("]");
                break;
        }
        sb.append(" from " + info.getName());
        sb.append(" of type ");
        sb.append(tcMessage.getMessageType().getToken());
        sb.append(": ");
        sb.append(String.valueOf(tcMessage.getMessage()));

        return sb.toString();
    }
	 */

    //helpers
    public double getPathDistance(Location loc1, Location loc2){
        return  aStar.getDistance(navigation.getNearestNavPoint(loc1), navigation.getNearestNavPoint(loc2));
    }
    public double getPathDistance(Location loc1){
        return  aStar.getDistance(navigation.getNearestNavPoint(loc1), info.getNearestNavPoint());
    }

    //GETTERS AND SETTERS

    public Player getEnemy() {
        return enemy;
    }

    public void setEnemy(Player enemy) {
        this.enemy = enemy;
    }

    public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public Item getTargetItem() {
		return targetItem;
	}

	public void setTargetItem(Item targetItem) {
		this.targetItem = targetItem;
	}

	public Location getEnemyFlagLocation() {
		return enemyFlagLocation;
	}

	public void setEnemyFlagLocation(Location enemyFlagLocation) {
		this.enemyFlagLocation = enemyFlagLocation;
	}

	public Location getOurFlagLocation() {
		return ourFlagLocation;
	}

	public void setOurFlagLocation(Location ourFlagLocation) {
		this.ourFlagLocation = ourFlagLocation;
	}

	public Map<UnrealId, SupportType> getSupportRequests() {
		return supportRequests;
	}

	public void setSupportRequests(Map<UnrealId, SupportType> supportRequests) {
		this.supportRequests = supportRequests;
	}

    public Map<UnrealId, TCTeammateInfo> getTeammates() {
        return teammates;
    }

    public boolean isSniper() {
        return sniper;
    }

    public void setSniper(boolean sniper) {
        this.sniper = sniper;
    }

    public boolean isSupporting() {
        return supporting;
    }

    public void setSupporting(boolean supporting) {
        this.supporting = supporting;
    }

    public boolean isFlagUnknown() {
        return flagUnknown;
    }

    public void setFlagUnknown(boolean flagUnknown) {
        this.flagUnknown = flagUnknown;
    }

    public boolean isCamper() {
        return camper;
    }

    public void setCamper(boolean camper) {
        this.camper = camper;
    }

    // //////////
	// //////////////
	// BOT KILLED //
	// //////////////
	// //////////

	@Override
	public void botKilled(BotKilled event)
	{

		reset();		
	}

	// //////////////////////////////////////////
	/*
	1. parametr: 2017 ... letosni rok a zaroven verze meho skriptu
	2. parametr: 0 nebo 1, kde 0 je cerveny tym a 1 je modry
	3. parametr: 4 až 7 je skill botu (v turnaji na konci semestru bude nastaven na 4 až 6)
	4. parametr: 3 až 8 ... to je počet botů v týmů a i když letos to při turnaji bude vždy 5, tak pro hodnocení vyučujícím a pro testování počítejte i s trojčlenými týmy tj. s hodnotou 3 - viz níže)
	5. parametr: localhost nebo xx.xx.xx.xx ... IP adresa serveru
	 */
	public static void main(String args[]) throws PogamutException {
		try {
			team = Integer.valueOf(args[1]);
			int skill = Integer.valueOf(args[2]);
			TEAM_SIZE = Integer.valueOf(args[3]);
			String host = args[4];
			// setup for 3 - 8 bots
			UT2004BotParameters botsParamSetup[] = {
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 0")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 1")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 2")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 3")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 4")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 5")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 6")),
					new CTFBotParams().setSkill(skill).setTeam(team).setAgentId(new AgentId("dobripet - 7"))
			};
			// get required number of bots
			//UT2004BotParameters botsParams[] = Arrays.copyOf(botsParamSetup, TEAM_SIZE);
			UT2004BotParameters botsParams[] = Arrays.copyOf(botsParamSetup, TEAM_SIZE);
			// run bots
			new UT2004BotRunner<UT2004Bot, UT2004BotParameters>(CTFBot.class, "dobripet").setMain(true).setHost(host).setLogLevel(Level.WARNING).startAgents(botsParams);
		}catch (Exception e ){
			System.err.println("Error during bot setup!" );
			e.printStackTrace();
		}
	}

}
