package tux2.MonsterBox;

import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class MonsterBoxPlayerListener implements Listener {
	
	MonsterBox plugin;
	
	public MonsterBoxPlayerListener(MonsterBox plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event)  {
		if(!event.isCancelled()) {
			ItemStack is = event.getPlayer().getItemInHand();
			Player player = event.getPlayer();
			if(is.getTypeId() == 383 && event.getClickedBlock() != null && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getTypeId() == 52) {
				if(plugin.hasPermissions(player, "monsterbox.eggset")) {
					if(plugin.bl.intmobs.containsKey(new Integer(is.getDurability()))) {
						String type = plugin.bl.intmobs.get(new Integer(is.getDurability()));
						CreatureSpawner theSpawner = (CreatureSpawner) event.getClickedBlock().getState();
				    	CreatureType ct = CreatureType.fromName(type);
				        if (ct != null && plugin.hasPermissions(player, "monsterbox.eggspawn." + type.toLowerCase())) {
				        	theSpawner.setCreatureType(ct);
							player.sendMessage(ChatColor.DARK_GREEN + "KERPOW! That is now a " + ChatColor.RED + type.toLowerCase() + ChatColor.DARK_GREEN + " spawner.");
				        	//Now that we set the spawner type let's remove the egg, but only if the player is in survival mode...
				        	if(player.getGameMode() == GameMode.SURVIVAL) {
					        	is.setAmount(is.getAmount() - 1);
				        	}
				        }
					}
				}
			}else if(plugin.usespout != null && is.getType().getId() == plugin.tool && event.getClickedBlock() != null && event.getClickedBlock().getTypeId() == 52) {
				SpoutPlayer splayer = SpoutManager.getPlayer(player);
				if(splayer.isSpoutCraftEnabled() && plugin.hasPermissions(player, "monsterbox.set")) {
					CreatureSpawner theSpawner = (CreatureSpawner) event.getClickedBlock().getState();
					String monster = theSpawner.getCreatureTypeId().toLowerCase();
					splayer.closeActiveWindow();
					plugin.ss.createMonsterGUI("This is currently a " + monster + " spawner.", !plugin.hasPermissions(splayer, "monsterbox.free"), splayer);
				}
			}
		}
		
	}
}
