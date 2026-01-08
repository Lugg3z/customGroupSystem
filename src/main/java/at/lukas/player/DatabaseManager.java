package at.lukas.player;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    // Getters and setters
    @Getter
    private ArrayList<String> groups = new ArrayList<>();
    @Getter
    private ArrayList<String> prefixes = new ArrayList<>();

    private HashMap<UUID, String> player_prefixes = new HashMap<>();

    public DatabaseManager() {
        // Initialize default groups
        groups.add("Default");
        groups.add("Vip");
        groups.add("Admin");

        prefixes.add("&7[Member]");
        prefixes.add("&6[VIP]");
        prefixes.add("&c[Admin]");
    }

    public boolean groupExists(String groupName) {
        return groups.contains(groupName);
    }

    public void createGroup(String name, String prefix) {
        groups.add(name);
        prefixes.add(prefix);
    }

    public String getPrefix(String groupName) {
        int index = groups.indexOf(groupName);
        if (index == -1) return "";
        return prefixes.get(index);
    }

    public void setUserGroup(UUID uuid, String groupName) {
        player_prefixes.put(uuid, getPrefix(groupName));
    }

    public String getPlayerPrefix(UUID uuid) {
        return player_prefixes.get(uuid);
    }

    public boolean deleteGroup(String groupName) {
        if(groupExists(groupName)){
            prefixes.remove(getPrefix(groupName));
            return groups.remove(groupName);
        }
        else{
            return false;
        }

        //Jeder user der diese rolle hat muss default bekommen
    }

    public boolean playerHasRole(UUID uuid) {
        return player_prefixes.containsKey(uuid);
    }

    public List<String> getAllGroups() {
        return groups;
    }
}
