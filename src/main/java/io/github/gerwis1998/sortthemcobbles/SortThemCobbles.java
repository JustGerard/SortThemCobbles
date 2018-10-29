package io.github.gerwis1998.sortthemcobbles;


import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;

public final class SortThemCobbles extends JavaPlugin {

    public ArrayList<User> users = new ArrayList<>();

    @Override
    public void onEnable() {
        try {
            YamlReader reader = new YamlReader(new FileReader("/plugins/stc/players.yml"));
            while(true){
                User user = reader.read(User.class);
                if(user == null) break;
                users.add(user);
            }
        } catch (FileNotFoundException | YamlException e) {
            File file = new File("/plugins/stc/players.yml");
            try {
                file.createNewFile();
                onEnable();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void onDisable(){
        try {
            YamlWriter writer = new YamlWriter(new FileWriter("/plugins/stc/players.yml"));
            if(users.size()>0){
                for (User user : users){
                    writer.write(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<ItemStack> sortItems(ArrayList<ItemStack> items){
        for(int i=0; i<items.size(); i++){
            for(int j=1; j<items.size()-i; j++){
                ItemStack item1 = items.get(j-1);
                ItemStack item2 = items.get(j);
                //if items are the same type
                if(item1.getType().toString().compareTo(item2.getType().toString()) == 0){
                    boolean removed = false;
                    if(item1.getAmount() == item2.getAmount()){//if there is the same amount of item1 and item2
                        if(item1.getAmount() < item1.getMaxStackSize()){//if there isn't a full stack of item1 and item2
                            int diff = item1.getMaxStackSize() - item1.getAmount();
                            if(item2.getAmount() == diff){
                                item1.setAmount(item1.getMaxStackSize());
                                items.remove(item2);
                                return sortItems(items);
                            }
                            else if(item2.getAmount() < diff){
                                item1.setAmount(item1.getAmount()*2);
                                items.remove(item2);
                                return sortItems(items);
                            }
                            else{
                                item1.setAmount(item1.getAmount() + diff);
                                item2.setAmount(item2.getAmount() - diff);
                            }
                        }
                    }
                    else{
                        if(item1.getAmount() < item2.getAmount()){//if there is more of item2 then swap them
                            ItemStack temp = item1;
                            item1 = item2;
                            item2 = temp;
                        }
                        int diff = item1.getMaxStackSize() - item1.getAmount();
                        if(item2.getAmount() <= diff){//if items can be merged completely
                            item1.setAmount(item1.getAmount() + item2.getAmount());
                            items.remove(item2);
                            return sortItems(items);
                        }
                        else{//if there is more of item2 then difference in maxStackSize and amount of item1
                            item1.setAmount(item1.getMaxStackSize());
                            item2.setAmount(item2.getAmount() - diff);
                        }
                    }
                    items.set(j-1,item1);
                    items.set(j,item2);
                }
                //if items have other types
                else{
                    if(item1.getType().toString().compareTo(item2.getType().toString()) > 0){
                        items.set(j-1,item2);
                        items.set(j,item1);
                    }
                }
            }
        }
        return items;
    }

    @EventHandler
    public void InventorySort(InventoryOpenEvent event){
        Player player = (Player) event.getPlayer();
        boolean isInPlayers = false;
        User currUser = null;
        for(User user : users){
            if(player.getName() == user.name){
                isInPlayers = true;
                currUser = user;
                break;
            }
        }
        if(isInPlayers){
            if(currUser.sortInventory && currUser != null){
                PlayerInventory inventory = player.getInventory();
                ArrayList<ItemStack> items = new ArrayList<>();
                for(int i=9; i<=36; i++){
                    try {
                        if(inventory.getItem(i).getAmount() > 0) {
                            items.add(inventory.getItem(i));
                        }
                    } catch (Exception e) { }
                }items = sortItems(items);
                for(int j=9; j<=36; j++){
                    inventory.clear(j);
                }
                int i=9;
                for(ItemStack item: items){
                    inventory.setItem(i,item);
                    i++;
                }
            }
        }
    }

    @EventHandler
    public void ChestSort(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        if(block.getType() == Material.CHEST && action == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            boolean isInPlayers = false;
            User currUser = null;
            for (User user : users) {
                if (user.name.equalsIgnoreCase(player.getName())) {
                    isInPlayers = true;
                    currUser = user;
                    break;
                }
            }
            if (isInPlayers && currUser != null) {
                if (currUser.sortChests) {
                    Chest chest = (Chest) block.getState();
                    Inventory inventory = chest.getInventory();
                    ArrayList<ItemStack> items = new ArrayList<>();
                    for(int i=0; i<inventory.getSize(); i++){
                        try {
                            items.add(inventory.getItem(i));
                        } catch (Exception e) { }
                    }
                    items = sortItems(items);
                    inventory.clear();
                    for(ItemStack item : items){
                        inventory.addItem(item);
                    }
                }
            }
        }
    }

    public void addNewUser(CommandSender sender){
        User user = new User(sender.getName(),true,true);
        users.add(user);
        sender.sendMessage("Sorting chests and inventory has been enabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            if (cmd.getName().equalsIgnoreCase("stc")) {
                if(args.length == 0){
                    sender.sendMessage("/stc inventory - enables sorting inventory");
                    sender.sendMessage("/stc chest - enables sorting chests");
                    return true;
                }
                if(args.length > 1){
                    sender.sendMessage("Too many arguments!");
                    return false;
                }
                switch (args[0]){
                    case "inventory":{
                        boolean isInPlayers = false;
                        for(User user : users){
                            if(user.name.equalsIgnoreCase(sender.getName())){
                                isInPlayers = true;
                                user.sortInventory = !user.sortInventory;
                                if (user.sortInventory) {
                                    sender.sendMessage("Sorting inventory has been enabled.");
                                } else {
                                    sender.sendMessage("Sorting inventory has been disabled.");
                                }
                                break;
                            }
                        }
                        if(!isInPlayers){
                            addNewUser(sender);
                            return true;
                        }
                        return true;
                    }
                    case "chest":{
                        boolean isInPlayers = false;
                        for(User user : users){
                            if(user.name.equalsIgnoreCase(sender.getName())){
                                isInPlayers = true;
                                user.sortChests = !user.sortChests;
                                if(user.sortChests){
                                    sender.sendMessage("Sorting chests has been enabled.");
                                } else {
                                    sender.sendMessage("Sorting chests has been disabled.");
                                }
                                break;
                            }
                        }
                        if(!isInPlayers){
                            addNewUser(sender);
                            return true;
                        }
                        return true;
                    }
                    default:{
                        sender.sendMessage("type /stc to get help");
                        return true;
                    }
                }
            }
            else return false;
        }
        else {
            sender.sendMessage("You must be a player!");
            return false;
        }
    }
}
