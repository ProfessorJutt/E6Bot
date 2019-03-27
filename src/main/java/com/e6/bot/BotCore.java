package com.e6.bot;

import com.e6.bot.config.Configuration;
import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.event.*;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import com.github.theholywaffle.teamspeak3.api.wrapper.ServerQueryInfo;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

class BotCore implements Runnable {

    private Thread thread;
    private TS3Api api;
    private TS3Query query;
    private int botClientId;
    private int botCurrentChannelId;
    private boolean running = true;
    private static Random random = new Random();
    private static Configuration appConfig;

    BotCore() {

        // Configuration file that allows users to edit the bot's behavior.
        appConfig = new Configuration();

        // Bot connection config.
        final TS3Config config = new TS3Config();
        config.setHost(appConfig.getServerAddress());


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
        this.query = new TS3Query(config);
        this.query.connect();

        this.api.addTS3Listeners(new TS3Listener() {
            @Override
            public void onTextMessage(TextMessageEvent textMessageEvent) {
                if (textMessageEvent.getTargetMode().getIndex() == 1 && textMessageEvent.getInvokerId() != botClientId) {
                    if (textMessageEvent.getMessage().startsWith("!")) {
                        handleMessages(textMessageEvent);
                    }
                }

            }

            @Override
            public void onClientJoin(ClientJoinEvent clientJoinEvent) {
                api.sendPrivateMessage(clientJoinEvent.getClientId(), appConfig.getUserJoinMessage());
            }

            @Override
            public void onClientLeave(ClientLeaveEvent clientLeaveEvent) {

            }

            @Override
            public void onServerEdit(ServerEditedEvent serverEditedEvent) {

            }

            @Override
            public void onChannelEdit(ChannelEditedEvent channelEditedEvent) {

            }

            @Override
            public void onChannelDescriptionChanged(ChannelDescriptionEditedEvent channelDescriptionEditedEvent) {

            }

            @Override
            public void onClientMoved(ClientMovedEvent clientMovedEvent) {
                if (clientMovedEvent.getClientId() == botClientId) {
                    botCurrentChannelId = clientMovedEvent.getTargetChannelId();
                }
            }

            @Override
            public void onChannelCreate(ChannelCreateEvent channelCreateEvent) {

            }

            @Override
            public void onChannelDeleted(ChannelDeletedEvent channelDeletedEvent) {

            }

            @Override
            public void onChannelMoved(ChannelMovedEvent channelMovedEvent) {

            }

            @Override
            public void onChannelPasswordChanged(ChannelPasswordChangedEvent channelPasswordChangedEvent) {

            }

            @Override
            public void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent privilegeKeyUsedEvent) {

            }
        });
    }

    private void loginLogic(TS3Api tsApi) {
        this.api = tsApi;
        this.api.login(appConfig.getUsername(), appConfig.getPassword());
        this.api.selectVirtualServerById(appConfig.getVirtualServerId());
        this.api.setNickname(appConfig.getBotNickname());
        this.api.registerAllEvents();
        ServerQueryInfo info = this.api.whoAmI();
        this.botClientId = info.getId();
        this.botCurrentChannelId = info.getChannelId();
    }

    @Override
    public void run() {
        while (this.running) {
            try {
                for (Client c : this.api.getClients()) {
                    // Break out of iteration if the user is in one of the safe groups.
                    if (isInSafeGroup(c)) continue;

                    // Logic to move idle users.
                    if (c.getIdleTime() >= appConfig.getAfkTimeToMove()) {
                        if (c.getChannelId() != appConfig.getAfkChannelId()) {
                            System.out.println(c.getNickname() + " has been AFK to long and is being moved...");
                            this.api.moveClient(c.getId(), appConfig.getAfkChannelId());
                        }
                    }
                }

                try {
                    Thread.sleep(appConfig.getBotCheckDelay());
                } catch (InterruptedException ex) {
                    // This is an expected exception since we interrupt the thread when it is sleeping to exit the app.
                }
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                System.out.println("Likely connection issues...");
            }
        }
    }

    private boolean isInSafeGroup(Client client) {
        for (int safeGroupId : appConfig.getSafeGroups()) if (client.isInServerGroup(safeGroupId)) return true;
        return false;
    }

    private void handleMessages(TextMessageEvent event) {
        switch (event.getMessage().toLowerCase()) {
            case "!roll":
                this.executeRoll(event);
                break;
            case "!raffle":
                this.executeRaffle(event);
                break;
            default:
                // Send help when they fuck up...
                this.api.sendPrivateMessage(event.getInvokerId(), "Available Commands: !roll, !raffle");
                break;
        }
    }

    private void executeRoll(TextMessageEvent event) {

    }

    private void executeRaffle(TextMessageEvent event) {
        int raffleChannelId = this.api.getClientByNameExact(event.getInvokerName(), false).getChannelId();
        Map<Integer, List<Client>> clientChannelMap = this.api.getClients().stream().collect(Collectors.groupingBy(Client::getChannelId));

        clientChannelMap.get(raffleChannelId).removeIf(client ->
                client.getId() == this.api.whoAmI().getId() || this.isInRaffleBlacklistGroup(client));

        if (clientChannelMap.get(raffleChannelId).size() == 0) {
            this.sendChannelMessage(raffleChannelId, "There isn't anyone in here that is allowed to win a raffle you IDIOT!");
            return;
        }

        String winner = clientChannelMap.get(raffleChannelId)
                .get(random.nextInt(clientChannelMap.get(raffleChannelId).size())).getNickname();

        if (winner.equals("Jutt")) winner = "the almighty bald old washed up man himself... Jutt";
        if (winner.equals("Pugsly")) winner = "the one and only dog with breathing issues... Pugsly";

        this.sendChannelMessage(raffleChannelId, "The winner of the raffle is " + winner + "! Congrats!");
    }

    private boolean isInRaffleBlacklistGroup(Client client) {
        for (int raffleGroupId : appConfig.getRaffleGroupBlacklist())
            if (client.isInServerGroup(raffleGroupId)) return true;
        return false;
    }

    private void sendChannelMessage(int channelId, String message) {
        if (this.botCurrentChannelId != channelId) this.api.sendChannelMessage(channelId, message);
        else this.api.sendChannelMessage(message);
    }

    void start() {
        if (this.thread != null) return;

        this.thread = new Thread(this, "BotCoreThread");
        this.thread.start();
    }

    void Close() {
        this.running = false;
        this.thread.interrupt();
        this.query.exit();
    }
}
