package com.e6.bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class CommandLineHandler implements Runnable {

    private static BufferedReader input;
    private Thread thread;
    private boolean exit = false;

    boolean isExit() {
        return exit;
    }

    @Override
    public void run() {
        while(true) {
            try {
                String message = input.readLine();
                if (message.toLowerCase().equals("exit")) {
                    exit = true;
                    break;
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    void start () {
        if (thread == null) {
            thread = new Thread (this, "CommandThread");
            thread.start ();
        }
    }

    CommandLineHandler() {
        input = new BufferedReader(new InputStreamReader(System.in));
    }

}
