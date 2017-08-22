package cz.zcu.fav.kiv.dobripet;

import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import cz.cuni.amis.pathfinding.map.IPFMapView;
import cz.cuni.amis.pogamut.base.agent.impl.AgentId;
import cz.cuni.amis.pogamut.base.agent.module.comm.PogamutJVMComm;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.PathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.impl.PrecomputedPathFuture;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassEventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.object.event.WorldObjectUpdatedEvent;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.sensor.NavigationGraphBuilder;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004MapTweaks;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
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
import cz.cuni.amis.pogamut.ut2004.teamcomm.server.UT2004TCServer;
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
 * @author Jimmy
 * @author dobripet
 */
@AgentScoped
public class CTFBot extends UT2004BotTCController<UT2004Bot> {
    /**
     * Global anti-stuck mechanism. When this counter reaches a certain
     * constant, the bot's mind gets a {@link CTFBot#reset()}.
     */
    private int notMoving = 0;

    // base name of bot
    private String botName;
    // team
    private static int team;
    // status info cooldown
	private final Cooldown statusCD = new Cooldown(1500);
    // combat heatup
    private final Heatup combatHU = new Heatup(1000);
	// goal manager
	private GoalManager goalManager = null;
	// number of bots in team
	private static int TEAM_SIZE;
	// bots initialized
    private static int botsInitialized = 0;
    // bot role
	private Role role;
    // bring enemy flag
    private DefendOurFlag defendOurFlagGoal;
    private Snipe snipeGoal;
    //last goal
    private int lastGoal;
	// current navigation target item
	private Item targetItem;
    // currently hunted player
    private Player enemy;
    // current enemy carry
    private Player enemyCarry;
    //where is player going
    private Location navigationTarget;


    private Location hideCarryLocation;
    private Location rendezvousLocation;

	private Map<UnrealId, SupportType> supportRequests;
    private Map<UnrealId, TCTeammateInfo> teammates;
    private Map<UnrealId, Tuple2<TCEnemyInfo, Cooldown>> enemies;

	// last known enemy flag location
	private Location enemyFlagLocation;
	// last known our flag location
	private Location ourFlagLocation;
	// if flag was not at last known position
    private boolean flagUnknown = true;

	//helper to determine if bot holded flag last logic
	private boolean isCarry;
    private boolean sniper = false;
    // if bot is camping at flag
    private boolean camper = false;
    private boolean supporting;
    //target of current path
    private Location pathTarget;

	// accelerated path executor
	private UT2004AcceleratedPathExecutor pathExecutor;
	// removes unwalkable paths
	private UT2004PathAutoFixer autoFixer;

	// taboo list to forbid items
	private TabooSet<Item> tabooItems = null;


    //<editor-fold desc="INITIALIZATION">

    @Override
    public void prepareBot(UT2004Bot bot) {
        bot.getLogger().setLevel(Level.WARNING);
        bot.getLogger().addDefaultFileHandler(new File("CTFBot"+botsInitialized+"-"+team+".txt"));
        //set base name
        botName = bot.getName();
        tabooItems = new TabooSet<Item>(bot);

        pathExecutor = ((UT2004AcceleratedPathExecutor) navigation.getPathExecutor());
        pathExecutor.removeAllStuckDetectors();

        pathExecutor.addStuckDetector(new AccUT2004TimeStuckDetector(bot, 3000, 10000));
        pathExecutor.addStuckDetector(new AccUT2004PositionStuckDetector(bot));
        pathExecutor.addStuckDetector(new AccUT2004DistanceStuckDetector(bot));
        // auto-removes wrong navigation links between navpoints
        autoFixer = new UT2004PathAutoFixer(bot, pathExecutor, fwMap, aStar, navBuilder);
        //handle changes in path execution
        pathExecutor.getState().addStrongListener(new FlagListener<IPathExecutorState>() {
            @Override
            public void flagChanged(IPathExecutorState changedValue) {
                pathExecutorStateChange(changedValue.getState());
            }
        });
        // setup smart shooting
        // general preferences, best for long range
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
        getWeaponPrefs().newPrefsRange(100)
                .add(UT2004ItemType.SHIELD_GUN, true)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.LINK_GUN, false)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.BIO_RIFLE, true);
        // < 2.5m
        getWeaponPrefs().newPrefsRange(250)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.LINK_GUN, false)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.BIO_RIFLE, true);

        // < 5m
        getWeaponPrefs().newPrefsRange(500)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.FLAK_CANNON, true)
                .add(UT2004ItemType.LINK_GUN, false)
                .add(UT2004ItemType.BIO_RIFLE, true);

        // < 10m
        getWeaponPrefs().newPrefsRange(1000)
                .add(UT2004ItemType.MINIGUN, true)
                .add(UT2004ItemType.LINK_GUN, true)
                .add(UT2004ItemType.SHOCK_RIFLE, true)
                .add(UT2004ItemType.ROCKET_LAUNCHER, true);

        // < 25m
        getWeaponPrefs().newPrefsRange(2500)
                .add(UT2004ItemType.SHOCK_RIFLE, true)
                .add(UT2004ItemType.MINIGUN, false);

        //init sets
        supportRequests = new HashMap<UnrealId, SupportType>();
        teammates = new HashMap<UnrealId, TCTeammateInfo>();
        enemies = new HashMap<UnrealId, Tuple2<TCEnemyInfo, Cooldown>>();
    }

   @Override
    public void mapInfoObtained() {
        mapTweaks.register("CTF-Citadel", new UT2004MapTweaks.IMapTweak() {
            @Override
            public void tweak(NavigationGraphBuilder navigationGraphBuilder) {
                navBuilder.removeEdgesBetween("CTF-Citadel.PathNode99", "CTF-Citadel.JumpSpot27");
                navBuilder.removeEdgesBetween("CTF-Citadel.PathNode36", "CTF-Citadel.JumpSpot5");
                navBuilder.removeEdgesBetween("CTF-Citadel.PathNode75", "CTF-Citadel.JumpSpot26");
                navBuilder.removeEdgesBetween("CTF-Citadel.PathNode64", "CTF-Citadel.jumpspot9");
                navBuilder.removeEdgesBetween("CTF-Citadel.InventorySpot179", "CTF-Citadel.InventorySpot193");
                navBuilder.removeEdge("CTF-Citadel.PathNode26", "CTF-Citadel.PathNode23");
            }
        });
        mapTweaks.register("CTF-BP2-Concentrate", new UT2004MapTweaks.IMapTweak() {
            @Override
            public void tweak(NavigationGraphBuilder navigationGraphBuilder) {
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.InventorySpot59", "CTF-BP2-Concentrate.PathNode81");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.xRedFlagBase1", "CTF-BP2-Concentrate.PathNode81");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.xRedFlagBase1", "CTF-BP2-Concentrate.PathNode74");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.AssaultPath12", "CTF-BP2-Concentrate.PathNode81");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.AssaultPath12", "CTF-BP2-Concentrate.PathNode74");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot2", "CTF-BP2-Concentrate.PathNode74");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot2", "CTF-BP2-Concentrate.PathNode81");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot11", "CTF-BP2-Concentrate.JumpSpot13");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot11", "CTF-BP2-Concentrate.PathNode40");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot11", "CTF-BP2-Concentrate.JumpSpot14");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot11", "CTF-BP2-Concentrate.PathNode31");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot14", "CTF-BP2-Concentrate.PathNode75");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot13", "CTF-BP2-Concentrate.PathNode75");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.InventorySpot55", "CTF-BP2-Concentrate.PathNode44");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.xBlueFlagBase0", "CTF-BP2-Concentrate.PathNode44");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.xBlueFlagBase0", "CTF-BP2-Concentrate.PathNode0");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot3", "CTF-BP2-Concentrate.PathNode0");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot3", "CTF-BP2-Concentrate.PathNode44");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot4", "CTF-BP2-Concentrate.JumpSpot6");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot4", "CTF-BP2-Concentrate.JumpSpot5");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot4", "CTF-BP2-Concentrate.PathNode18");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.JumpSpot4", "CTF-BP2-Concentrate.PathNode30");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.PathNode39", "CTF-BP2-Concentrate.JumpSpot6");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.PathNode39", "CTF-BP2-Concentrate.JumpSpot5");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.AssaultPath5", "CTF-BP2-Concentrate.PathNode44");
                navBuilder.removeEdgesBetween("CTF-BP2-Concentrate.AssaultPath5", "CTF-BP2-Concentrate.PathNode0");
            }
        });
       mapTweaks.register("CTF-Maul", new UT2004MapTweaks.IMapTweak() {
           @Override
           public void tweak(NavigationGraphBuilder navigationGraphBuilder) {
               //blue
               navBuilder.removeEdge("CTF-Maul.PathNode10", "CTF-Maul.JumpSpot6");
               navBuilder.removeEdge("CTF-Maul.PathNode12", "CTF-Maul.JumpSpot6");
               navBuilder.removeEdge("CTF-Maul.PathNode143", "CTF-Maul.JumpSpot6");
               navBuilder.removeEdge("CTF-Maul.PathNode63", "CTF-Maul.JumpSpot6");
               navBuilder.removeEdge("CTF-Maul.PathNode142", "CTF-Maul.JumpSpot6");
               navBuilder.removeEdge("CTF-Maul.PathNode10", "CTF-Maul.JumpSpot20");
               navBuilder.removeEdge("CTF-Maul.PathNode63", "CTF-Maul.JumpSpot20");
               navBuilder.removeEdge("CTF-Maul.PathNode6", "CTF-Maul.JumpSpot20");
               navBuilder.removeEdge("CTF-Maul.PathNode64", "CTF-Maul.JumpSpot20");
               navBuilder.removeEdge("CTF-Maul.InventorySpot801", "CTF-Maul.JumpSpot20");
               navBuilder.removeEdge("CTF-Maul.PlayerStart1", "CTF-Maul.JumpSpot7");
               navBuilder.removeEdge("CTF-Maul.InventorySpot801", "CTF-Maul.JumpSpot7");
               navBuilder.removeEdge("CTF-Maul.PathNode48", "CTF-Maul.JumpSpot7");
               navBuilder.removeEdge("CTF-Maul.PathNode64", "CTF-Maul.JumpSpot7");

               //red
               navBuilder.removeEdge("CTF-Maul.PlayerStart8", "CTF-Maul.JumpSpot2");
               navBuilder.removeEdge("CTF-Maul.InventorySpot807", "CTF-Maul.JumpSpot2");
               navBuilder.removeEdge("CTF-Maul.PathNode92", "CTF-Maul.JumpSpot2");
               navBuilder.removeEdge("CTF-Maul.PathNode66", "CTF-Maul.JumpSpot2");
               navBuilder.removeEdge("CTF-Maul.PathNode67", "CTF-Maul.JumpSpot2");
               navBuilder.removeEdge("CTF-Maul.PathNode66", "CTF-Maul.JumpSpot18");
               navBuilder.removeEdge("CTF-Maul.PathNode93", "CTF-Maul.JumpSpot18");
               navBuilder.removeEdge("CTF-Maul.PathNode67", "CTF-Maul.JumpSpot18");
               navBuilder.removeEdge("CTF-Maul.PathNode76", "CTF-Maul.JumpSpot18");
               navBuilder.removeEdge("CTF-Maul.PathNode95", "CTF-Maul.JumpSpot18");
               navBuilder.removeEdge("CTF-Maul.PathNode67", "CTF-Maul.JumpSpot3");
               navBuilder.removeEdge("CTF-Maul.PathNode77", "CTF-Maul.JumpSpot3");
               navBuilder.removeEdge("CTF-Maul.PathNode95", "CTF-Maul.JumpSpot3");
               navBuilder.removeEdge("CTF-Maul.PathNode78", "CTF-Maul.JumpSpot3");
               navBuilder.removeEdge("CTF-Maul.PathNode96", "CTF-Maul.JumpSpot3");
           }
       });

    }


    @Override
	public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
		//select starting role
		if(botsInitialized % 2 == 0){
		    role = Role.ATTACKER;
        } else{
		    role = Role.DEFENDER;
        }
        botsInitialized++;
        // create goal manager
        goalManager = new GoalManager(this);
        // add common goal
        goalManager.addGoal(new PickItem(this));
		goalManager.addGoal(new BringEnemyFlag(this));
        goalManager.addGoal(defendOurFlagGoal = new DefendOurFlag(this));
        goalManager.addGoal(new Support(this));
        goalManager.addGoal(new HuntDown(this));
        List<Location> defendSpots = new ArrayList<Location>();
        List<Location> defendFocusSpots = new ArrayList<Location>();
		if(navBuilder.isMapName("CTF-Citadel")){
            //navBuilder.removeEdgesBetween("CTF-Citadel.PathNode75", "CTF-Citadel.JumpSpot26");
            //draw.drawLine(Color.cyan, navPoints.getNavPoint("CTF-Citadel.PathNode75").getLocation(), navPoints.getNavPoint("CTF-Citadel.JumpSpot26").getLocation());
            //draw.drawLine(Color.cyan, navPoints.getNavPoint("CTF-Citadel.PathNode64").getLocation(), navPoints.getNavPoint("CTF-Citadel.jumpspot9").getLocation());
			tabooItems.add(items.getItem("CTF-Citadel.SuperHealthCharger0"));
            tabooItems.add(items.getItem("CTF-Citadel.SuperHealthCharger1"));
            tabooItems.addAll(items.getAllItems(UT2004ItemType.BIO_RIFLE_AMMO).values());
            List<Location> snipingSpots = new ArrayList<Location>();
            List<Location> snipingFocusSpots = new ArrayList<Location>();
			if(team == 0){
                rendezvousLocation = navPoints.getNavPoint("CTF-Citadel.InventorySpot175").getLocation();
                hideCarryLocation = navPoints.getNavPoint("CTF-Citadel.AssaultPath1").getLocation();
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
                rendezvousLocation = navPoints.getNavPoint("CTF-Citadel.InventorySpot172").getLocation();
                hideCarryLocation = navPoints.getNavPoint("CTF-Citadel.AssaultPath0").getLocation();
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

		}
		if(navBuilder.isMapName("CTF-BP2-Concentrate")) {
            if(team == 0) {
                rendezvousLocation = navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot6").getLocation();
                hideCarryLocation = navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot29").getLocation();
                defendSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.JumpSpot11").getLocation());
                defendSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot50").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.JumpSpot11").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.AssaultPath12").getLocation());
            }else{
                rendezvousLocation = navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot10").getLocation();
                hideCarryLocation = navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot36").getLocation();
                defendSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.JumpSpot4").getLocation());
                defendSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot41").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.AssaultPath5").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot37").getLocation());
            }
            defendOurFlagGoal.setDefendSpots(defendSpots);
            defendOurFlagGoal.setDefendFocusSpots(defendFocusSpots);
        }
        if(navBuilder.isMapName("CTF-Maul")) {
            if(team == 0) {
                rendezvousLocation = navPoints.getNavPoint("CTF-Maul.InventorySpot809").getLocation();
                hideCarryLocation = navPoints.getNavPoint("CTF-Maul.InventorySpot829").getLocation();
                defendSpots.add(navPoints.getNavPoint("CTF-Maul.InventorySpot814").getLocation());
                defendSpots.add(navPoints.getNavPoint("CTF-Maul.InventorySpot810").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Maul.PathNode46").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Maul.JumpSpot18").getLocation());
            }else{
                rendezvousLocation = navPoints.getNavPoint("CTF-Maul.InventorySpot804").getLocation();
                hideCarryLocation = navPoints.getNavPoint("CTF-Maul.InventorySpot827").getLocation();
                defendSpots.add(navPoints.getNavPoint("CTF-Maul.InventorySpot819").getLocation());
                defendSpots.add(navPoints.getNavPoint("CTF-Maul.InventorySpot805").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Maul.PathNode54").getLocation());
                defendFocusSpots.add(navPoints.getNavPoint("CTF-Maul.JumpSpot20").getLocation());
            }
            defendOurFlagGoal.setDefendSpots(defendSpots);
            defendOurFlagGoal.setDefendFocusSpots(defendFocusSpots);
        }
	}
    /**
     * Returns parameters of the bot.
     * @return
     */
    public CTFBotParams getParams() {
        if (!(bot.getParams() instanceof CTFBotParams)) return null;
        return (CTFBotParams)bot.getParams();
    }
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
        //navigation.setLogLevel(Level.FINEST);
    }

    @Override
    public void botShutdown() {
        PogamutJVMComm.getInstance().unregisterAgent(bot);
    }

    //</editor-fold>

    //<editor-fold desc="FIGHT">

    /**
     * Updates fight and shooting
     */
	private void updateFight() {
        //no visibe enemies
        if(!players.canSeeEnemies()){
            stopShooting();
            return;
        }
        if(enemyCarry!= null && enemyCarry.isVisible()){
            enemy = enemyCarry;
        } else{
            // no carry just fight
            enemy = players.getNearestVisibleEnemy();
        }
        shoot(enemy);
	}

	private void shoot(Player target){
        if (target!= null && target.isVisible()) {
            /*bullet wasting
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
             */
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

    //</editor-fold>

    //<editor-fold desc="COMMUNCATION">

    /**
     * Process picked up items from teammate
     * @param msg
     */
    @EventListener(eventClass = TCItemPickedUp.class)
    private void processTCItemPickedUp(TCItemPickedUp msg) {
        Item item = items.getItem(msg.getId());
        if(item == null){
            log.warning( "TEAMMATE PICKED UP NULL");
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
        if(teammates.get(msg.getBotID()) == null || teammates.get(msg.getBotID()).getUpdatedTime() < msg.getUpdatedTime()){
            teammates.put(msg.getBotID(),msg);
        }
        //not pickup same item as other bot
        if(msg.getTargetItemId() != null){
            Item i = items.getItem(msg.getTargetItemId());
            if(i != null && !i.getDescriptor().getItemCategory().equals(ItemType.Category.WEAPON)){
                tabooItems.add(i,1.5);
            }
        }
        log.info( "TEAMMATE INFO UPDATE: " + msg.getName()+ " "+ msg.getLocation() +" " + msg.getUpdatedTime());
    }

    /**
     * Process info update of enemy
     * @param msg
     */
    @EventListener(eventClass = TCEnemyInfo.class)
    public void processTCEnemyInfo(TCEnemyInfo msg) {
        //update enemy info
        if(enemies.get(msg.getBotID()) == null || enemies.get(msg.getBotID()).getFirst().getUpdatedTime() < msg.getUpdatedTime()){
            enemies.put(msg.getBotID(), new Tuple2<TCEnemyInfo, Cooldown>(msg, new Cooldown(400)));
        }
        //update carry
        if(msg.isCarry()){
            enemyCarry = players.getPlayer(msg.getBotID());
        }
        log.info( "ENEMY INFO UPDATE: " + msg);
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
		log.info("FLAG LOCATION UPDATED " + enemyFlagLocation + " " + ourFlagLocation);
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
        log.info("SUPPORT UPDATED " + msg.getBotId() + " " + msg.getSupportType() + " " + msg.isRevoking() );
	}


    //</editor-fold>

    //<editor-fold desc="LOGIC">
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
        if(!tcClient.isConnected()){
            log.severe("COMM IS DOWN");
        }
        //antistuck check
        if(!info.isMoving()) {
            notMoving++;
            if(notMoving > 30) {
                log.warning("STUCK: NOT MOVING ");
                reset();
                return;
            }
        } else {
            notMoving = 0;
        }
        //will be chosen after goal;
        targetItem = null;
        //flags processing
        log.info("CARRY " +isCarry + " " +ctf.isBotCarryingEnemyFlag());
        if(!ctf.isBotCarryingEnemyFlag() && isCarry){
            //cancel support request, not carry flag anymore
            tcClient.sendToTeamOthers(new TCSupportUpdate(info.getId(), null, true));
            bot.getLog().fine("SENDING SUPPORT UPDATE OFF");
        }

        isCarry = ctf.isBotCarryingEnemyFlag();
        sniper = false;
        // update location of visible friends
        /*for(Player player : players.getVisibleFriends().values()){
            if(teammates.get(player.getId()) == null){
                teammates.put(player.getId(), new TCTeammateInfo(player.getId(),null,player.getLocation(), null, null, false, false, info.getTime()));
                }else {
                teammates.get(player.getId()).setLocation(player.getLocation());
                teammates.get(player.getId()).setUpdatedTime(info.getTime());
            }
        }*/
        goalManager.executeBestGoal();

        //update fight
        updateFight();

        //notify others on cooldown to prevent spamm
        if(statusCD.isCool()) {
            UnrealId targetItemId = null;
            if (targetItem != null) {
                targetItemId = targetItem.getId();
            }
            tcClient.sendToTeamOthers(new TCTeammateInfo(info.getId(), role, info.getLocation(), targetItemId, botName, sniper, camper, info.getTime()));
            statusCD.use();
        }


        // camping, turn around
        if(!navigation.isNavigating() && !info.isShooting() && !players.canSeeEnemies())
        {
            move.turnHorizontal(120);
        }

        /*log.finest("TEAMMATES LOCATIONS: ");
        for( TCTeammateInfo teammateInfo : teammates.values()){
            log.finest(teammateInfo.getName() + " " + teammateInfo.getLocation() + " " + teammateInfo.getUpdatedTime());
        }*/
    }
    //</editor-fold>

    //<editor-fold desc="NAVIGATION">
    /**
     * Cover map definition
     */
    private class CoverMapView implements IPFMapView<NavPoint> {
        @Override
        public Collection<NavPoint> getExtraNeighbors(NavPoint node, Collection<NavPoint> mapNeighbors) {
            return null;
        }

        @Override
        public int getNodeExtraCost(NavPoint node, int mapCost) {
            int penalty = 0;
            for (Tuple2<TCEnemyInfo, Cooldown> tuple : enemies.values()) {
                TCEnemyInfo player = tuple.getFirst();
                if (player.getLocation() != null) {
                    if (node.getLocation().getDistance(player.getLocation()) <= 500.0D) {
                        penalty += 1000;
                    }
                    if (visibility.isVisible(node, player.getLocation())) {
                        penalty += 500;
                    }
                }

            }
            return penalty;
        }

        @Override
        public int getArcExtraCost(NavPoint nodeFrom, NavPoint nodeTo, int mapCost) {
            return 0;
        }

        @Override
        public boolean isNodeOpened(NavPoint node) {
            // ALL NODES ARE OPENED
            return true;
        }

        @Override
        public boolean isArcOpened(NavPoint nodeFrom, NavPoint nodeTo) {
            // ALL ARCS ARE OPENED
            NavPointNeighbourLink link = nodeFrom.getOutgoingEdges().get(nodeTo.getId());
            if(!fwMap.checkLink(link)) {
                return false;
            }
                return true;
        }
    }

    /**
     * Create cover path to target location
     * @param location location to run
     * @return
     */
    private PrecomputedPathFuture<NavPoint> generateCoverPath(ILocated location) {
        NavPoint startNav = info.getNearestNavPoint();
        NavPoint targetNav = navPoints.getNearestNavPoint(location);
        AStarResult<NavPoint> result = aStar.findPath(startNav, targetNav, new CoverMapView());
        PrecomputedPathFuture<NavPoint> pathFuture = new PrecomputedPathFuture<NavPoint>(startNav, targetNav, result.getPath());
        return pathFuture;
    }

    /**
     * Basic navigation
     * @param target where to navigate
     */
    public void goTo(ILocated target) {
        //draw.clearAll();
		if (target == null) {
			log.warning("GOTO TARGET NULL");
			navigation.stopNavigation();
			return;
		}
		pathTarget = target.getLocation();
        if(pathTarget == null){
            log.warning("GOTO TARGET LOCATION NULL");
            navigation.stopNavigation();
        }
        ///NavPoint nearestTarget = navPoints.getNearestNavPoint(target);
        if(navigation.isNavigating() && target == navigationTarget){
            log.fine("GOTO CONTINUE" +target.getLocation() + " " +target);
            //drawPath(navigation.getCurrentPathCopy());
            return;
        }

        navigation.navigate(target);
        navigationTarget = target.getLocation();
        //drawPath(navigation.getCurrentPathCopy());
        log.info("GOTO " +target.getLocation() + " " +target);
	}

    /**
     * Cover navigation, try to stay away from enemies
     * @param target where to navigate
     */
    public void goToCover(ILocated target) {
        //draw.clearAll();
	    if (target == null) {
            log.warning("GOTO COVER TARGET NULL");
            navigation.stopNavigation();
            return;
        }
        pathTarget = target.getLocation();
        if(pathTarget == null){
            log.warning("GOTO COVER TARGET LOCATION NULL");
            navigation.stopNavigation();
            return;
        }
        if((navigation.isNavigating() || pathExecutor.isExecuting()) && target == navigationTarget){
            log.fine("GOTO COVER CONTINUE");
            //drawPath(navigation.getCurrentPathCopy());
            return;
        }
        PrecomputedPathFuture<NavPoint> path = generateCoverPath(target);
        if (path == null) {
            log.warning("GOTO COVER PATH NULL");
            goTo(target);
            return;
        }
        navigation.navigate((IPathFuture)path);
        navigationTarget = target.getLocation();
        //drawPath(navigation.getCurrentPathCopy());
        log.info("GOTO COVER " +target.getLocation() + " " +target);
    }

    /**
     * Navigation to moving target
     * @param player to navigate
     */
	public void followPlayer(Player player){
	    if(player == null || player.getLocation() == null){
	        log.warning("FOLLOW PLAYER NULL");
	        return;
        }
	    navigation.navigate(player);
    }

    public boolean isCoverPathNavigating(){
        return (!pathExecutor.isExecuting() || !navigation.isNavigating());
    }
    //</editor-fold>

    //<editor-fold desc="HELPER METHODS">
/*
    private void drawPath(List<? extends ILocated> path) {
        log.info("DRAWING PATH, size = " + path.size());
        draw.setColor(Color.WHITE);
        for (int i = 1; i < path.size(); ++i) {
            draw.drawLine(path.get(i-1),  path.get(i));
        }
    }*/
    /**
     * Perform item pick
     * @param item
     */
    public void pickItem(Item item){
        targetItem = item;
        if (targetItem == null) {
            bot.getLog().warning("PICKITEM IS NULL");
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

    /**
     * Resets the state of the bot.
     */
    protected void reset() {
        notMoving = 0;
        enemy = null;
        targetItem = null;
        navigationTarget = null;
        navigation.stopNavigation();
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
    public double getPathDistance(Location loc1, Location loc2){
        return  aStar.getDistance(navigation.getNearestNavPoint(loc1), navigation.getNearestNavPoint(loc2));
    }
    public double getPathDistance(Location loc1){
        return  aStar.getDistance(navigation.getNearestNavPoint(loc1), info.getNearestNavPoint());
    }

    /**
     * Path executor has changed its state
     * {@link UT2004BotModuleController#getNavigation()} as well!).
     *
     * @param state
     */
    private void pathExecutorStateChange(PathExecutorState state) {
        switch(state) {
            case PATH_COMPUTATION_FAILED:
                // if navigating to item taboo it
                if (targetItem != null) {
                    log.warning("PATH FAILED WITH" + targetItem);
                    tabooItems.add(targetItem, 60);
                }
                break;

            case STUCK:
                // if navigating to item taboo it
                if (targetItem != null) {
                    log.warning("STUCK WTIH " + targetItem);
                    tabooItems.add(targetItem, 20);
                }
                break;

            case TARGET_REACHED:
                //item is not spawned, do not wait and notify others
                if (targetItem != null && pathTarget != null) {
                    tabooItems.add(targetItem, items.getItemRespawnTime(targetItem));
                    log.info("NOT SPAWNED " + targetItem);
                    if(targetItem.getId() == null){
                        log.warning("ID NULL TARGETREACHED");
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
    //</editor-fold>

    //<editor-fold desc="WORLD EVENTS">

    /**
     * Player update event, runs with logic start for visible players
     * @param event
     */
    @ObjectClassEventListener(eventClass = WorldObjectUpdatedEvent.class, objectClass = Player.class)
    private void playerUpdated(WorldObjectUpdatedEvent<Player> event) {
        Player player = (Player)event.getObject();
        //teammate
        if(player.getTeam() == team){
            //see player before first message, ignore and wait for first message
            if(teammates.get(player.getId()) == null){
                return;
            }
            if(player.getLocation() != null) {
                teammates.get(player.getId()).setLocation(player.getLocation());
                teammates.get(player.getId()).setUpdatedTime(info.getTime());
            }
        }else{
            //enemy player
            boolean carry = false;
            if(ctf.getOurFlag() != null && ctf.getOurFlag().getHolder() != null && ctf.getOurFlag().getHolder() == player.getId()){
                carry = true;
                enemyCarry = player;
            }
            if(enemies.get(player.getId()) == null){
                enemies.put(player.getId(), new Tuple2<TCEnemyInfo, Cooldown>(new TCEnemyInfo(player.getId(), player.getLocation(), carry, info.getTime()), new Cooldown(400)));
            }
            if(player.getLocation() != null) {
                enemies.get(player.getId()).getFirst().setLocation(player.getLocation());
                enemies.get(player.getId()).getFirst().setUpdatedTime(info.getTime());
                //notify rest of the team
                if(enemies.get(player.getId()).getSecond().isCool()) {
                    tcClient.sendToTeamOthers(enemies.get(player.getId()).getFirst());
                    enemies.get(player.getId()).getSecond().use();
                    log.info( "SEND ENEMY INFO UPDATE");
                }
            }
        }
    }


    /**
     * Flag update event, runs with logic start
     * @param event
     */
    @ObjectClassEventListener(eventClass = WorldObjectUpdatedEvent.class, objectClass = FlagInfo.class)
    private void flagUpdated(WorldObjectUpdatedEvent<FlagInfo> event) {
        FlagInfo flag = (FlagInfo)event.getObject();
        TCUpdateFlagLocation msg = null;
        //our flag
        log.info("FLAG UPD LOCATION" + flag.getLocation() + " " +flag.getHolder() + " " + flag.getState());
        if(flag.getTeam() == team){
            if(flag.getState().equals("home")){
                //no need to send message, next logic every bot will know and update their location
                flagUnknown = false;
                ourFlagLocation = ctf.getOurBase().getLocation();
                if(enemyCarry != null){
                    enemyCarry = null;
                }
                return;
            }
            //clear holder
            if(flag.getState().equals("dropped")){
                enemyCarry = null;
            }
            if(flag.getLocation() != null){
                flagUnknown = false;
                if(ourFlagLocation == flag.getLocation()){
                    //no need to notify anything
                    return;
                }
                //rewrite current holder
                if(flag.getHolder() != null){
                    enemyCarry = players.getPlayer(flag.getHolder());
                }
                ourFlagLocation = flag.getLocation();
                msg = new TCUpdateFlagLocation(null, ourFlagLocation);
            }

        }else{
            //enemy flag
            if(flag.getState().equals("home")){
                //no need to send message, next logic every bot will know and update their location
                enemyFlagLocation = ctf.getEnemyBase().getLocation();
                return;
            } else if(flag.getLocation() != null){
                if(enemyFlagLocation == flag.getLocation()){
                    //no need to notify anything
                    return;
                }
                enemyFlagLocation = flag.getLocation();
                msg = new TCUpdateFlagLocation(enemyFlagLocation, null);
            }
        }
        //send message if something new happened
        tcClient.sendToTeamOthers(msg);
        log.fine("FLAG LOCATION UPDATED SENT " + msg);
    }

    /**
     * Taboo picked items and notify team
     */
    @EventListener(eventClass = ItemPickedUp.class)
    private void itemPickedUp(ItemPickedUp event) {
        Item item = items.getItem(event.getId());
        if(item != null ){

            if(item.getId() == null){
                log.warning("ID NULL ITEMPICKUP");
                System.exit(0);
            }
            tabooItems.add(item, items.getItemRespawnTime(item));
            tcClient.sendToTeamOthers(new TCItemPickedUp(item.getId()));
        }else {
            log.warning("PICKUP EVENT ITEM NULL");
        }
    }

    /**
     * Killed enemy, clearing target
     */
    @EventListener(eventClass = PlayerKilled.class)
    private void playerKilled(PlayerKilled event) {
        //if player got killed clear heat
        if(enemy != null && event.getId() == enemy.getId()) {
            enemy = null;
        }
        if(enemyCarry != null && event.getId() == enemyCarry.getId()){
            enemyCarry = null;
        }
    }

    /**
     * Evading
     * @param event
     */
    @EventListener(eventClass = Bumped.class)
    private void bumped(Bumped event) {
        log.info("BUMP");
        move.strafeLeft(100, event.getLocation());
    }

    /**
     * Was Damaged, check back if needed
     * @param event
     */
    @EventListener(eventClass = BotDamaged.class)
    private void damaged(BotDamaged event) {
        if(event.isCausedByWorld()){
            return;
        }
        if(combatHU.isCool() && players.getVisibleEnemies().size() == 0){
            //check your back some one shoots on bot but can't see enemy
            move.turnHorizontal(180);
            log.fine("GOT SHOT TURNING AROUND");
        }
        combatHU.heat();
    }

    /**
	 * Rocket dodging
	 * @param event
    */
	@ObjectClassEventListener(objectClass = IncomingProjectile.class, eventClass = WorldObjectUpdatedEvent.class)
	private void incomingProjectile(WorldObjectUpdatedEvent<IncomingProjectile> event) {
        //dodge only rockets
	    if(!event.getObject().getType().equals(UT2004ItemType.ROCKET_LAUNCHER_PROJECTILE.getName())){
            return;
        }
	    //don't dodge my projectiles  or too close ones
	    if (event.getObject().getOrigin().getDistance(info.getLocation()) < 300){
	        return;
        }
		if(event.getObject().getLocation().getDistance(info.getLocation()) < 750){
			if(random.nextDouble() < 0.5) {
				move.dodgeLeft(info.getLocation().add(info.getRotation().toLocation()), false);
			} else{
				move.dodgeRight(info.getLocation().add(info.getRotation().toLocation()), false);
			}
		}
	}

    @Override
    public void botKilled(BotKilled event) {
        reset();
    }
    //</editor-fold>

    //<editor-fold desc="GETTERS AND SETTERS">

    public TabooSet<Item> getTaboo() {
        return tabooItems;
    }

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

    public int getLastGoal() {
        return lastGoal;
    }

    public void setLastGoal(int lastGoal) {
        this.lastGoal = lastGoal;
    }

    public Player getEnemyCarry() {
        return enemyCarry;
    }

    public void setEnemyCarry(Player enemyCarry) {
        this.enemyCarry = enemyCarry;
    }

    public Map<UnrealId, Tuple2<TCEnemyInfo, Cooldown>> getEnemies() {
        return enemies;
    }

    public void setEnemies(Map<UnrealId, Tuple2<TCEnemyInfo, Cooldown>> enemies) {
        this.enemies = enemies;
    }

    public Location getHideCarryLocation() {
        return hideCarryLocation;
    }

    public void setHideCarryLocation(Location hideCarryLocation) {
        this.hideCarryLocation = hideCarryLocation;
    }

    public Location getRendezvousLocation() {
        return rendezvousLocation;
    }

    public void setRendezvousLocation(Location rendezvousLocation) {
        this.rendezvousLocation = rendezvousLocation;
    }

    //</editor-fold>

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
			UT2004BotParameters botsParams[] = Arrays.copyOf(botsParamSetup, TEAM_SIZE);
			// Start TC Server first...
            UT2004TCServer tcServer = UT2004TCServer.startTCServer();
			// run bots
			new UT2004BotRunner<UT2004Bot, UT2004BotParameters>(CTFBot.class, "dobripet").setMain(true).setHost(host).setLogLevel(Level.WARNING).startAgents(botsParams);
		}catch (Exception e ){
			System.err.println("Error during bot setup!" );
			e.printStackTrace();
		}
	}

}
