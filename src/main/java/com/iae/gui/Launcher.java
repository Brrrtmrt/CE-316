package com.iae.gui;

/**
 * A workaround class to bypass the classic JavaFX 11+ "missing runtime components" error.
 * This class must NOT extend javafx.application.Application.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApplication.main(args);
    }
}