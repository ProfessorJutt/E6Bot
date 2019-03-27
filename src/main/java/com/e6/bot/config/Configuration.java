package com.e6.bot.config;

import org.apache.commons.configuration.XMLConfiguration;

import java.util.List;

public class Configuration {


    private String serverAddress = "127.0.0.1";
    private String botNickname = "SwagMonster";
    private String username = "serveradmin";
    private String password = "Y22sR4oS";
    private String userJoinMessage = "Welcome!";
    private int virtualServerId = 1;
    private int afkChannelId = 8;
    private long afkTimeToMove = 30000;
    private long botCheckDelay = 10000;
    private int[] safeGroups;
    private int[] raffleGroupBlacklist;

    public int[] getRaffleGroupBlacklist() {
        return raffleGroupBlacklist;
    }
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
    public String getUserJoinMessage() {
        return userJoinMessage;
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

    @SuppressWarnings("unchecked")
    public Configuration() {

        try {

            XMLConfiguration config = new XMLConfiguration("config.xml");

            this.serverAddress = config.getString("serverAddress");
            this.username = config.getString("username");
            this.password = config.getString("password");
            this.afkChannelId = config.getInt("afkChannelId");
            this.virtualServerId = config.getInt("virtualServerId");
            this.botNickname = config.getString("botNickname");
            this.afkTimeToMove = convertToMil(config.getLong("afkTimeToMove"));
            this.botCheckDelay = convertToMil(config.getLong("botCheckDelay"));
            this.userJoinMessage = config.getString("userJoinMessage");

            List raffleBlacklist = config.getList("raffleGroupBlacklist");
            this.raffleGroupBlacklist = raffleBlacklist.stream().mapToInt(i -> Integer.parseInt((String) i)).toArray();

            List safeGroupList = config.getList("safeGroups");
            this.safeGroups = safeGroupList.stream().mapToInt(i -> Integer.parseInt((String) i)).toArray();

        }
        catch (Exception ex) {
            System.out.println("Failed to load configuration!");
        }


    }
    //convert to milliseconds
    private long convertToMil(long minutes){
        return minutes * 60 * 1000;

    }
}
