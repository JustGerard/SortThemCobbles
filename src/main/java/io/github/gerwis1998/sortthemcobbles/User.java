package io.github.gerwis1998.sortthemcobbles;
public class User {
    public String name;
    public boolean sortInventory;
    public boolean sortChests;

    User(String name, boolean sortInventory, boolean sortChests){
        this.name=name;
        this.sortInventory = sortInventory;
        this.sortChests = sortChests;
    }
}
