package com.cagritasoz;

import com.cagritasoz.model.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class JudgeApp {

    public static void main( String[] args ) {

        File file = new File("D:\\Coding\\JAVA\\Judge\\sample-configs\\c.json");

        UUID id = UUID.randomUUID();

        Configuration configuration = new Configuration(
                id,
                "C Programming",
                "GCC-based C compilation and execution",
                Path.of("gcc"),
                null,
                List.of("{compilerPath}", "{sourceFiles}", "-o", "{compiledOutputName}"),
                List.of("{compiledOutputPath}", "{args}"),
                List.of("{compilerPath}", "--version"),
                "*.c",
                "main.exe"
        );

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, configuration);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}
