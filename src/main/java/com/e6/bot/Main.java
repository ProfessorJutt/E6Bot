package com.e6.bot;

public class Main {

    public static void main(String[] args) {

        // Command Line thread (allows user to input commands to the bot via command line.)
        CommandLineHandler appStart = new CommandLineHandler();
        appStart.start();

        // Bot thread that checks AFK status every x minutes.
        BotCore bot = new BotCore();
        bot.start();

        // This will trigger the app to close
        while (!appStart.isExit())
        {
            // Preventing CPU rape.
            try {
                Thread.sleep(1000);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Disconnects the bot.
        bot.Close();
    }

}