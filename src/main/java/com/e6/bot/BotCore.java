package com.e6.bot;

import com.e6.bot.commands.CommandLoader;
import com.e6.bot.config.Configuration;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.TextMessageTargetMode;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

class BotCore implements Runnable {

    private Thread thread;
    private TS3Api api;
    private TS3Query query;
    private boolean running = true;
    private static Configuration appConfig;
    private static CommandLoader customCommands;
    private static Random random = new Random();

    private int botClientId;
    private int botCurrentChannel;

    private List<Client> clientList;
    private long lastMoveTimestamp;
    private long lastEnterTimestamp;
    private long lastLeaveTimestamp;
    private long lastClientRefresh;

    BotCore () {
        // Configuration file that allows users to edit the bot's behavior.
        appConfig = new Configuration();
        customCommands = new CommandLoader();

        // Bot connection config.
        final TS3Config config = new TS3Config();
        config.setHost(appConfig.getServerAddress());
        config.setDebugLevel(Level.ALL);

        // Reconnection logic
        config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
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

        // Permission to send messages.
        api.addClientPermission(api.whoAmI().getDatabaseId(), "i_client_needed_private_textmessage_power", 50, true);

        api.addTS3Listeners(new TS3Listener() {
            @Override
            public void onTextMessage(TextMessageEvent textMessageEvent) {
                TextMessageTargetMode targetMode = textMessageEvent.getTargetMode();
                if (textMessageEvent.getMessage().startsWith("!")) {
                    if (targetMode.getIndex() == 1 && textMessageEvent.getInvokerId() != botClientId) {
                        handleMessages(textMessageEvent);
                    }
                }
            }
            @Override
            public void onClientJoin(ClientJoinEvent clientJoinEvent) {
                api.sendPrivateMessage(clientJoinEvent.getClientId(), appConfig.getUserJoinMessage());
                lastEnterTimestamp = System.currentTimeMillis();
            }
            @Override
            public void onClientLeave(ClientLeaveEvent clientLeaveEvent) {
                lastLeaveTimestamp = System.currentTimeMillis();
            }
            @Override
            public void onClientMoved(ClientMovedEvent clientMovedEvent) {
                if (clientMovedEvent.getClientId() == botClientId) {
                    botCurrentChannel = clientMovedEvent.getTargetChannelId();
                }
                lastMoveTimestamp = System.currentTimeMillis();
            }
            @Override
            public void onServerEdit(ServerEditedEvent serverEditedEvent) {}
            @Override
            public void onChannelEdit(ChannelEditedEvent channelEditedEvent) {}
            @Override
            public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent channelDescriptionEditedEvent) { }
            @Override
            public void onChannelCreate(ChannelCreateEvent channelCreateEvent) { }
            @Override
            public void onChannelDeleted(ChannelDeletedEvent channelDeletedEvent) { }
            @Override
            public void onChannelMoved(ChannelMovedEvent channelMovedEvent) { }
            @Override
            public void onChannelPasswordChanged(ChannelPasswordChangedEvent channelPasswordChangedEvent) { }
            @Override
            public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent privilegeKeyUsedEvent) { }
        });

    }

    // This gets called every time that the bot connects (allows for reconnecting when the server goes down)
    private void loginLogic(TS3Api tsApi) {
        api = tsApi;
        api.login(appConfig.getUsername(), appConfig.getPassword());
        api.selectVirtualServerById(appConfig.getVirtualServerId());
        api.setNickname(appConfig.getBotNickname());
        api.registerAllEvents();

        ServerQueryInfo info = api.whoAmI();
        botClientId = info.getId();
        botCurrentChannel = info.getChannelId();
    }

    @Override
    public void run() {
        // The run thread handles the checks for afk users.
        int[] safeGroups = appConfig.getSafeGroups();

        while (running) {
            try {

                clientList = api.getClients();
                lastClientRefresh = System.currentTimeMillis();

                for (Client c : clientList) {

                    boolean isProtected = false;
                    int[] groups = c.getServerGroups();

                    // Checking if the user is safe from moves.
                    for (int clientGroupId : groups) {
                        // Looping through the safe groups and kicking out of the loop if there is a match.
                        for (int safeGroup : safeGroups) if (clientGroupId == safeGroup) isProtected = true;
                        if (isProtected) break;
                    }

                    // Is it the bot you're trying to move?
                    if (c.getId() == botClientId) isProtected = true;

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

    // When a valid message gets passed in this method handles it.
    private void handleMessages(TextMessageEvent event) {

        String message = event.getMessage().toLowerCase();
        Client sender = null;

        if (message.equals("!roll") || message.equals("!raffle")) {
            if (lastMoveTimestamp > lastClientRefresh || lastEnterTimestamp > lastClientRefresh || lastLeaveTimestamp > lastClientRefresh) {
                clientList = api.getClients();
                lastClientRefresh = System.currentTimeMillis();
            }

            for (Client c : clientList) {
                if (event.getInvokerId() == c.getId()) {
                    sender = c;
                    break;
                }
            }

            if (sender == null) {
                sender = api.getClientInfo(event.getInvokerId());
            }

            if (message.equals("!roll")) {
                int roll = (int)(Math.random()*100);
                if (roll == 100) {
                    sendChannelMessage("Holy hot damn! " + sender.getNickname() + " rolled a " + roll, sender.getChannelId());
                }
                else if (roll == 0) {
                    sendChannelMessage(sender.getNickname() + " rolled a " + roll + "... I think you should win out of pity for being so bad at rolling.", sender.getChannelId());
                }
                else if (roll > 90) {
                    sendChannelMessage("Nice! " + sender.getNickname() + " rolled a " + roll, sender.getChannelId());
                }
                else if (roll < 10) {
                    sendChannelMessage("Ouch... " + sender.getNickname() + " rolled a " + roll, sender.getChannelId());
                }
                else {
                    sendChannelMessage(sender.getNickname() + " rolled a " + roll, sender.getChannelId());
                }
            }
            else if (message.equals("!raffle")) {

                List<Client> raffleEntries = new ArrayList<>();

                for (Client c : clientList) if (c.getChannelId() == sender.getChannelId()) raffleEntries.add(c);

                if (raffleEntries.size() == 0) {
                    sendChannelMessage("The winner is " + sender.getNickname() + "! Congrats!", sender.getChannelId());
                }
                else {
                    Client winner = raffleEntries.get(random.nextInt(raffleEntries.size()));
                    sendChannelMessage("The winner of the raffle is " + winner.getNickname() + "! Congrats!", sender.getChannelId());
                }
            }

        }
        else {
            // Help
            if (message.equals("!help")) api.sendPrivateMessage(event.getInvokerId(), "Available Commands: !roll, !raffle" + customCommands.getAvailableCommands());
            // Custom Commands loading
            else api.sendPrivateMessage(event.getInvokerId(), customCommands.getCommandText(message));
        }
    }

    // This completes the check for channel messages to make sure the bot is inside the channel it's sending messages to.
    private void sendChannelMessage(String message, int channelId) {
        if (botCurrentChannel != channelId) api.sendChannelMessage(channelId, message);
        else api.sendChannelMessage(message);
    }

    // External calls use the start method to create a thread of this class. The static thread name will stop the app from spawning multiple bots somehow.
    void start () {
        if (thread == null) {
            thread = new Thread (this, "BotCoreThread");
            thread.start ();
        }
    }

    // Clearing everything up and exiting the bot cleanly.
    void Close() {
        running = false;
        thread.interrupt();
        query.exit();
    }

}
