package tux2.MonsterBox;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.getspout.spout.Spout;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.register.Register;
import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Methods;

/**
 * MonsterBox for Bukkit
 *
 * @author tux2
 */
public class MonsterBox extends JavaPlugin {
    //private final MonsterBoxPlayerListener playerListener = new MonsterBoxPlayerListener(this);
    //private final MonsterBoxBlockListener blockListener = new MonsterBoxBlockListener(this);
    private final MonsterBoxServerListener serverListener = new MonsterBoxServerListener(this);
    private final HashMap<Player, Boolean> debugees = new HashMap<Player, Boolean>();
    private final ConcurrentHashMap<String, Double> mobprice = new ConcurrentHashMap<String, Double>();
    private static PermissionHandler Permissions;
    public MonsterBoxBlockListener bl;
    public Register iConomy = null;
	boolean useiconomy = false;
	public double iconomyprice = 0.0;
	public Spout usespout = null;
	public boolean separateprices = false;
	public int tool = Material.GOLD_SWORD.getId();
	public int buttonwidth = 100;
	public String version = "0.4";
	public SpoutStuff ss = null;
    public HashSet<Byte> transparentBlocks = new HashSet<Byte>();
    private ConcurrentHashMap<String, String> mobcase = new ConcurrentHashMap<String, String>();
    public MonsterBox() {
        super();
        loadconfig();
        loadprices();
        
      //Setting transparent blocks.
        transparentBlocks.add((byte) 0); // Air
        transparentBlocks.add((byte) 8); // Water
        transparentBlocks.add((byte) 9); // Stationary Water
        transparentBlocks.add((byte) 20); // Glass
        transparentBlocks.add((byte) 30); // Cobweb
        transparentBlocks.add((byte) 65); // Ladder
        transparentBlocks.add((byte) 66); // Rail
        transparentBlocks.add((byte) 78); // Snow
        transparentBlocks.add((byte) 83); // Sugar Cane
        transparentBlocks.add((byte) 101); // Iron Bars
        transparentBlocks.add((byte) 102); // Glass Pane
        transparentBlocks.add((byte) 106); // Vines

        // NOTE: Event registration should be done in onEnable not here as all events are unregistered when a plugin is disabled
    }

   

    private void loadprices() {

		File folder = new File("plugins/MonsterBox");

		// check for existing file
		File configFile = new File("plugins/MonsterBox/prices.ini");
		
		//if it exists, let's read it, if it doesn't, let's create it.
		if (configFile.exists()) {
			try {
				mobprice.clear();
				Properties theprices = new Properties();
				theprices.load(new FileInputStream(configFile));
				Iterator<Entry<Object, Object>> iprices = theprices.entrySet().iterator();
				while(iprices.hasNext()) {
					Entry<Object, Object> price = iprices.next();
					try {
						mobprice.put(price.getKey().toString().toLowerCase(), new Double(price.getValue().toString()));
					}catch (NumberFormatException ex) {
						System.out.println("[MonsterBox] Unable to parse the value for " + price.getKey().toString());
					}
				}
			} catch (IOException e) {
				
			}
			//A quick and dirty way to see if there are any new mobs we need to add to the list
			if(mobprice.size() < CreatureType.values().length) {
				System.out.println("[MonsterBox] - New mobs found! Updating prices.ini");
				createprices();
			}
		}else {
			System.out.println("[MonsterBox] Price file not found");
			folder.mkdir();

			System.out.println("[MonsterBox] - creating file prices.ini");
			createprices();
		}
		
	}



	private void createprices() {
		try {
			BufferedWriter outChannel = new BufferedWriter(new FileWriter("plugins/MonsterBox/prices.ini"));
			outChannel.write("#This config file contains all the separate prices for all the mobs\n" +
					"# if the option separateprices is true\n" +
					"\n" +
					"\n");
			CreatureType[] mobs = CreatureType.values();
			for(CreatureType mob : mobs) {
				outChannel.write(mob.getName() + " = " + String.valueOf(getMobPrice(mob.getName())) + "\n");
			}
			outChannel.close();
		} catch (Exception e) {
			System.out.println("[MonsterBox] - file creation failed, using defaults.");
		}
		
	}



	public void onEnable() {
    	setupPermissions();
    	setupSpout();
    	setupMobCase();
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        bl = new MonsterBoxBlockListener(this);
        MonsterBoxPlayerListener pl = new MonsterBoxPlayerListener(this);
        if(useiconomy ) {
            pm.registerEvents(serverListener, this);
        }
        pm.registerEvents(bl, this);
        pm.registerEvents(pl, this);
        if(usespout != null) {
        	pm.registerEvent(Type.CUSTOM_EVENT, new MonsterBoxScreenListener(this), Priority.Normal, this);
        	ss = new SpoutStuff(this);
        }
        MonsterBoxCommands commandL = new MonsterBoxCommands(this);
        PluginCommand batchcommand = this.getCommand("mbox");
		batchcommand.setExecutor(commandL);
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
        System.out.println( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }
    public void onDisable() {

        // NOTE: All registered events are automatically unregistered when a plugin is disabled

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        System.out.println("MonsterBox disabled!");
    }
    public boolean isDebugging(final Player player) {
        if (debugees.containsKey(player)) {
            return debugees.get(player);
        } else {
            return false;
        }
    }

    public void setDebugging(final Player player, final boolean value) {
        debugees.put(player, value);
    }
    
    private void setupPermissions() {
        Plugin permissions = this.getServer().getPluginManager().getPlugin("Permissions");

        if (Permissions == null) {
            if (permissions != null) {
                Permissions = ((Permissions)permissions).getHandler();
            } else {
            }
        }
    }
    
    private void setupSpout() {
    	Plugin p = getServer().getPluginManager().getPlugin("Spout");
		if(p == null){
			usespout = null;
			System.out.println("[MonsterBox] Spout not detected. Disabling spout support.");
		} else {
			usespout = (Spout)p;
			System.out.println("[MonsterBox] Spout detected. Spout support enabled.");
		}
    }
    
    public boolean hasPermissions(Player player, String node) {
        if (Permissions != null) {
            return Permissions.has(player, node);
        } else {
            return player.hasPermission(node);
        }
    }
    
    private void loadconfig() {
		File folder = new File("plugins/MonsterBox");

		// check for existing file
		File configFile = new File("plugins/MonsterBox/settings.ini");
		
		//if it exists, let's read it, if it doesn't, let's create it.
		if (configFile.exists()) {
			try {
				Properties themapSettings = new Properties();
				themapSettings.load(new FileInputStream(configFile));
		        
		        String iconomy = themapSettings.getProperty("useEconomy", "false");
		        String price = themapSettings.getProperty("price", "0.0");
		        String sprices = themapSettings.getProperty("separateprices", "false");
		        String swidth = themapSettings.getProperty("buttonwidth", "100");
		        String stool = themapSettings.getProperty("changetool", String.valueOf(Material.GOLD_SWORD.getId()));
		        //If the version isn't set, the file must be at 0.2
		        String theversion = themapSettings.getProperty("version", "0.1");
			    
			    useiconomy = stringToBool(iconomy);
			    separateprices = stringToBool(sprices);
			    try {
			    	tool = Integer.parseInt(stool.trim());
			    } catch (Exception ex) {
			    	
			    }
			    try {
			    	buttonwidth = Integer.parseInt(swidth.trim());
			    } catch (Exception ex) {
			    	
			    }
			    try {
			    	iconomyprice = Double.parseDouble(price.trim());
			    } catch (Exception ex) {
			    	
			    }
			    //Let's see if we need to upgrade the config file
			    double dbversion = 0.1;
			    try {
			    	dbversion = Double.parseDouble(theversion.trim());
			    } catch (Exception ex) {
			    	
			    }
			    if(dbversion < 0.4) {
			    	//If we are using the old config file let's convert that variable... otherwise we won't want to do that...
			    	if(dbversion == 0.1) {
				        String sconomy = themapSettings.getProperty("useiConomy", "false");
					    useiconomy = stringToBool(sconomy);
			    	}
			    	updateIni();
			    }
			} catch (IOException e) {
				
			}
		}else {
			System.out.println("[MonsterBox] Configuration file not found");

			System.out.println("[MonsterBox] + creating folder plugins/MonsterBox");
			folder.mkdir();

			System.out.println("[MonsterBox] - creating file settings.ini");
			updateIni();
		}
	}

	private void updateIni() {
		try {
			BufferedWriter outChannel = new BufferedWriter(new FileWriter("plugins/MonsterBox/settings.ini"));
			outChannel.write("#This is the main MonsterBox config file\n" +
					"#\n" +
					"# useiConomy: Charge to change monster spawner type using your economy system\n" +
					"useEconomy = " + useiconomy + "\n" +
					"# price: The price to change monster spawner type\n" +
					"price = " + iconomyprice + "\n\n" +
					"# separateprices: If you want separate prices for all the different types of mobs\n" +
					"# set this to true.\n" +
					"separateprices = " + separateprices + "\n" +
					"# changetool is the tool that opens up the spout gui for changing the monster spawner.\n" +
					"changetool = " + tool + "\n" +
					"# buttonwidth changes the width of the buttons in the spoutcraft gui, just in case the\n" +
					"# text doesn't fit for you.\n" +
					"buttonwidth = " + buttonwidth + "\n\n" +
					"#Do not change anything below this line unless you know what you are doing!\n" +
					"version = " + version );
			outChannel.close();
		} catch (Exception e) {
			System.out.println("[MonsterBox] - file creation failed, using defaults.");
		}
		
	}
	
	public boolean hasEconomy() {
		if(iConomy != null) {
			return Methods.hasMethod();
		}else {
			return false;
		}
	}
	
	public Method getEconomy() {
		if(iConomy != null) {
			return Methods.getMethod();
		}else {
			return null;
		}
	}
	
	private synchronized boolean stringToBool(String thebool) {
		boolean result;
		if (thebool.trim().equalsIgnoreCase("true") || thebool.trim().equalsIgnoreCase("yes")) {
	    	result = true;
	    } else {
	    	result = false;
	    }
		return result;
	}



	boolean setSpawner(Block targetBlock, String type) {
		try {
			CreatureSpawner theSpawner = (CreatureSpawner) targetBlock.getState();
			if (mobcase.containsKey(type.toLowerCase().trim())) {
	    		type = mobcase.get(type.toLowerCase().trim());
	    	}else {
	    		type = this.capitalCase(type);
	    	}
	    	CreatureType ct = CreatureType.fromName(type);
	        if (ct == null) {
	            return false;
	        }
	        theSpawner.setCreatureType(ct);
	        return true;
		}catch (Exception e) {
			return false;
		}
	}

	String capitalCase(String s)
	{
	    return s.toUpperCase().charAt(0) + s.toLowerCase().substring(1);
	}
	
	public double getMobPrice(String name) {
		if(separateprices && mobprice.containsKey(name.toLowerCase())) {
			return mobprice.get(name.toLowerCase()).doubleValue();
		}else {
			return iconomyprice;
		}
	}
	
	private void setupMobCase() {
		CreatureType[] mobs = CreatureType.values();
		for(CreatureType mob : mobs) {
			String mobname = mob.getName().trim();
			mobcase.put(mobname.toLowerCase(), mobname);
		}
	}
}

