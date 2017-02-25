package com.e6.bot.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;

public final class CommandLoader {

    private String root = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    private final File commandsDirectory = new File(root + "/CustomCommands");
    private List<CustomCommand> customCommandList = new ArrayList<>();

    // Constructor
    public CommandLoader() {
        LoadCommands();
    }

    // Instance methods
    private void LoadCommands() {
        // Grabbing all of the files.
        File[] fileList = commandsDirectory.listFiles();

        // Checking that there are any files.
        if (fileList != null) {
            for (final File fileEntry : fileList) {
                String fileName = fileEntry.getName();
                String commandName = "!" + FilenameUtils.removeExtension(fileName);
                Path filePath = new File(commandsDirectory.getPath() + "/" + fileName).toPath();

                String outputText = "";

                try {
                    List<String> lines = Files.readAllLines(filePath);

                    for (String line : lines) {
                        outputText += line + "\n" ;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                CustomCommand newCommand = new CustomCommand(commandName, outputText);
                customCommandList.add(newCommand);
            }
        }
    }

    // Public methods
    public String getCommandText(String command) {
        for (CustomCommand customCommand : customCommandList) {
            if (customCommand.getCommand().equals(command)) return customCommand.getOutput();
        }
        return "I did not recognise that command. Type !help for a list of available commands.";
    }

    public String getAvailableCommands() {
        String commands = "";

        for (CustomCommand customCommand : customCommandList) {
            commands += ", " + customCommand.getCommand();
        }

        return commands;
    }
}
