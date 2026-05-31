package com.iae.evaluation.steps;

import com.iae.domain.StudentSubmission;
import com.iae.evaluation.strategies.ExactMatchStrategy;
import com.iae.evaluation.strategies.IgnoreWhitespaceStrategy;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class CompareStepTest {

    private StudentSubmission withOutput(String output) {
        StudentSubmission sub = new StudentSubmission("s1", new File("dummy.zip"),new File("dummy_dir"));
        sub.setProgramOutput(output);
        return sub;
    }

    @Test
    void nullSubmission_returnsError() {
        StepResult result = new CompareStep(new ExactMatchStrategy(), "expected").execute(null);
        assertFalse(result.isSuccess());
        assertEquals("COMPARE", result.getStepName());
    }

    @Test
    void nullProgramOutput_returnsError() {
        StudentSubmission sub = new StudentSubmission("s1", new File("dummy.zip"),new File("dummy_dir"));
        StepResult result = new CompareStep(new ExactMatchStrategy(), "expected").execute(sub);
        assertFalse(result.isSuccess());
    }

    @Test
    void matchingOutput_returnsSuccess() {
        StepResult result = new CompareStep(new ExactMatchStrategy(), "Hello").execute(withOutput("Hello"));
        assertTrue(result.isSuccess());
        assertEquals("COMPARE", result.getStepName());
    }

    @Test
    void mismatchedOutput_returnsFailure() {
        StepResult result = new CompareStep(new ExactMatchStrategy(), "Hello").execute(withOutput("World"));
        assertFalse(result.isSuccess());
    }

    @Test
    void strategyIsApplied_ignoreWhitespace() {
        StepResult result = new CompareStep(new IgnoreWhitespaceStrategy(), "helloworld")
                .execute(withOutput("hello world"));
        assertTrue(result.isSuccess());
    }
}
