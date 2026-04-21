package com.judge.service;

import com.judge.model.Project;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public class ProjectRunner { // Orchestrates the whole system.

    private final ToolDetector toolDetector;

    private final ZipExtractor zipExtractor;

    private final SourceFileFinder sourceFileFinder;

    private final CommandBuilder commandBuilder;

    private final ProcessRunner processRunner;

    private final OutputComparator outputComparator;

    public void run(Project project) {
        try {
            extract(project);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void extract(Project project) throws Exception { //Setup submission List and extract zips.

        File submissionDirectory = new File(project.getSubmissionsDir());

        Path workDirPath = Path.of(project.getProjectDir(), "_work");

        Files.createDirectories(workDirPath); //IO exception

        File[] files = submissionDirectory.listFiles();

        if(files == null) {

        }
    }
}
