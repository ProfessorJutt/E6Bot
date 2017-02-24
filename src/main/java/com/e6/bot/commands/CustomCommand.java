package com.e6.bot.commands;

class CustomCommand {

    private String command;
    private String output;

    CustomCommand(String input, String outputText) {
        this.command = input;
        this.output = outputText;
    }

    String getCommand() {
        return command;
    }
    String getOutput() {
        return output;
    }

}
