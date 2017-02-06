package com.e6.bot;

import com.e6.bot.config.Configuration;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;

class BotCore implements Runnable {

    private Thread thread;
    private final TS3Api api;
    private final TS3Query query;
    private boolean running = true;
    private Configuration appConfig;

    BotCore () {
        // Configuration file that allows users to edit the bot's behavior.
        appConfig = new Configuration();

        // Bot connection config.
        final TS3Config config = new TS3Config();
        config.setHost(appConfig.getServerAddress());
        //config.setDebugLevel(Level.ALL);

        // Connecting to the server's query engine with the set configuration.
        query = new TS3Query(config);
        query.connect();

        // Initializing the api call handler and logging in.
        api = query.getApi();
        api.login(appConfig.getUsername(), appConfig.getPassword());
        api.selectVirtualServerById(appConfig.getVirtualServerId());
        api.setNickname(appConfig.getBotNickname());
    }

    @Override
    public void run() {
        while (running) {
            for (Client c : api.getClients()) {
                if (c.getIdleTime() >= appConfig.getAfkTimeToMove()) {
                    if (c.getChannelId() != 8) {
                        System.out.println(c.getNickname() + " has been AFK to long and is being moved...");
                        api.moveClient(c.getId(), 8);
                    }
                }
                //int[] groups = c.getServerGroups();
            }
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException ex) {
                // This is an expected exception since we interrupt the thread when it is sleeping to exit the app.
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
