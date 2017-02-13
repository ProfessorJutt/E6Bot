package com.e6.bot.config;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import java.util.List;

public class Configuration {


    private String serverAddress = "127.0.0.1";
    private String botNickname = "SwagMonster";
    private String username = "TestAdmin";
    private String password = "USbS3jbC";
    private int virtualServerId = 1;
    private int afkChannelId = 8;
    private long afkTimeToMove = 30000;
    private long botCheckDelay = 10000;
    private int[] safeGroups;

    public int getVirtualServerId() {
        return virtualServerId;
    }
    public int getAfkChannelId() {
        return afkChannelId;
    }
    public String getServerAddress() {
        return serverAddress;
    }
    public String getBotNickname() {
        return botNickname;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public long getAfkTimeToMove() {
        return afkTimeToMove;
    }
    public long getBotCheckDelay() {
        return botCheckDelay;
    }
    public int[] getSafeGroups() {
        return safeGroups;
    }

    public Configuration() {

        try {

            XMLConfiguration config = new XMLConfiguration("config.xml");

            serverAddress = config.getString("serverAddress");
            username = config.getString("username");
            password = config.getString("password");
            afkChannelId = config.getInt("afkChannelId");
            virtualServerId = config.getInt("virtualServerId");
            botNickname = config.getString("botNickname");

            afkTimeToMove = convertToMil(config.getLong("afkTimeToMove"));
            botCheckDelay = convertToMil(config.getLong("botCheckDelay"));

            @SuppressWarnings("unchecked")
            List<HierarchicalConfiguration> safeGroupList = config.configurationsAt("safeGroups.groupId");

            safeGroups = new int[safeGroupList.size()];

            int i = 0;
            for (HierarchicalConfiguration groupId : safeGroupList) {
                safeGroups[i] = Integer.parseInt(groupId.getRoot().getValue().toString());
                i++;
            }

        }
        catch (Exception ex) {
            System.out.println("Failed to load configuration!");
        }

    }

    // Convert to milliseconds
    private long convertToMil(long minutes){
        return minutes * 60 * 1000;
    }
}
