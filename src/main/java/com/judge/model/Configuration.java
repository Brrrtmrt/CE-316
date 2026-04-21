package com.judge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/*
-Specific for each programming language.
-
*/

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Configuration {
    private UUID id;
    private String name;
    private String description;

    private Path compilerPath;
    private Path interpreterPath;
    private List<String> compileCommandTemplate;
    private List<String> runCommandTemplate;
    private List<String> checkCommandTemplate;

    private String sourceFilePattern;
    private String compiledOutputName;


}
