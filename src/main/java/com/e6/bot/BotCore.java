package com.e6.bot;

import com.e6.bot.config.Configuration;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;

import java.util.logging.Level;

class BotCore implements Runnable {

    private Thread thread;
    private TS3Api api;
    private TS3Query query;
    private boolean running = true;
    private static Configuration appConfig;

    BotCore () {

        // Configuration file that allows users to edit the bot's behavior.
        appConfig = new Configuration();

        // Bot connection config.
        final TS3Config config = new TS3Config();
        config.setHost(appConfig.getServerAddress());
        config.setDebugLevel(Level.ALL);

        // Reconnect timer gets longer and longer.
        config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());

        // Reconnection logic
        config.setConnectionHandler(new ConnectionHandler() {
            @Override
            public void onConnect(TS3Query ts3Query) {
                loginLogic(ts3Query.getApi());
            }

            @Override
            public void onDisconnect(TS3Query ts3Query) {
                // Nada
            }
        });

        // Connecting to the server's query engine with the set configuration.
        query = new TS3Query(config);
        query.connect();

        loginLogic(query.getApi());
    }

    private void loginLogic(TS3Api tsApi) {
        api = tsApi;
        api.login(appConfig.getUsername(), appConfig.getPassword());
        api.selectVirtualServerById(appConfig.getVirtualServerId());
        api.setNickname(appConfig.getBotNickname());
    }

    @Override
    public void run() {

        int[] safeGroups = appConfig.getSafeGroups();

        while (running) {
            try {
                for (Client c : api.getClients()) {

                    boolean isProtected = false;
                    int[] groups = c.getServerGroups();

                    // Checking if the user is safe from moves.
                    for (int clientGroupId : groups) {
                        // Looping through the safe groups and kicking out of the loop if there is a match.
                        for (int safeGroup : safeGroups) if (clientGroupId == safeGroup) isProtected = true;
                        if (isProtected) break;
                    }

                    // Kicking out of this client's loop iteration if they are in a protected group.
                    if (isProtected) continue;

                    // Checking against idle time and then moving the user if the appropriate time has been met.
                    if (c.getIdleTime() >= appConfig.getAfkTimeToMove()) {
                        if (c.getChannelId() != appConfig.getAfkChannelId()) {
                            System.out.println(c.getNickname() + " has been AFK to long and is being moved...");
                            api.moveClient(c.getId(), appConfig.getAfkChannelId());
                        }
                    }
                }
                try {
                    Thread.sleep(appConfig.getBotCheckDelay());
                } catch (InterruptedException ex) {
                    // This is an expected exception since we interrupt the thread when it is sleeping to exit the app.
                    System.out.println("The user closed the application via the exit command.");
                }
            }
            catch (NullPointerException ex) {
                ex.printStackTrace();
                System.out.println("Likely connection issues...");
            }
        }
    }

    void start () {
        if (thread == null) {
            thread = new Thread (this, "BotCoreThread");
            thread.start ();
        }
    }

    void Close() {
        running = false;
        thread.interrupt();
        query.exit();
    }
}
