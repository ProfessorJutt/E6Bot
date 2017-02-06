package com.e6.bot.config;

import org.apache.commons.configuration.XMLConfiguration;

import java.util.List;

public class Configuration {

    private String serverAddress = "127.0.0.1";
    private String botNickname = "SwagMonster";
    private String username = "TestAdmin";
    private String password = "USbS3jbC";
    private int virtualServerId = 1;
    private long afkTimeToMove = 30000;
    private int[] safeGroups;

    public int getVirtualServerId() {
        return virtualServerId;
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
    public int[] getSafeGroups() {
        return safeGroups;
    }

    public Configuration() {

        try {

            XMLConfiguration config = new XMLConfiguration("config.xml");

            serverAddress = config.getString("serverAddress");
            username = config.getString("username");
            password = config.getString("password");
            afkTimeToMove = Long.parseLong(config.getString("afkTimeToMove"));
            virtualServerId = config.getInt("virtualServerId");
            botNickname = config.getString("botNickname");

            List<String> safeGroupList = config.getList("safeGroups");
            safeGroups = new int[safeGroupList.size()];

            for (int i = 0; i < safeGroupList.size(); i++) {
                safeGroups[i] = Integer.parseInt(safeGroupList.get(i));
            }

        }
        catch (Exception ex) {
            System.out.println("Failed to load configuration!");
        }


    }
}
