package com.judge.service;



import com.judge.model.Stage;
import com.judge.model.Submission;
import com.judge.model.SubmissionStatus;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ZipExtractor {

    public Submission extract(File file, Path workDirPath) {
        String fileName = file.getName();
        String filePath = file.getAbsolutePath();

        if(!fileName.toLowerCase().endsWith(".zip")) {
            return Submission.builder()
                    .id(fileName)
                    .filePath(filePath)
                    .fileName(fileName)
                    .submissionStatus(SubmissionStatus.SKIPPED)
                    .stage(Stage.DISCOVERY)
                    .processedAt(LocalDateTime.now())
                    .build();
        }

        String submissionId = fileName.substring(0, fileName.length() - ".zip".length());






    }


}
