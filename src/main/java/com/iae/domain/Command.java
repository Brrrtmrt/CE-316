package com.iae.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public record Command(String executable, List<String> arguments, File workingDirectory) {
    public Command(String executable, List<String> arguments, File workingDirectory) {
        this.executable = executable;
        this.arguments = new ArrayList<>(arguments);
        this.workingDirectory = workingDirectory;
    }

    public List<String> arguments() {
        return List.copyOf(arguments); // Assuming arguments won't be changed.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(executable);
        for (String arg : arguments) {
            sb.append(" ").append(arg);
        }
        return sb.toString();
    }
}
