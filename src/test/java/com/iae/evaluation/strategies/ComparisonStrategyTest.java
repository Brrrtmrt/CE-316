package com.iae.evaluation.strategies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonStrategyTest {

    @Nested
    class ExactMatchStrategyTest {

        private ExactMatchStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new ExactMatchStrategy();
        }

        @Test
        void bothNull_returnsTrue() {
            assertTrue(strategy.compare(null, null));
        }

        @Test
        void actualNull_returnsFalse() {
            assertFalse(strategy.compare(null, "hello"));
        }

        @Test
        void expectedNull_returnsFalse() {
            assertFalse(strategy.compare("hello", null));
        }

        @Test
        void bothEmpty_returnsTrue() {
            assertTrue(strategy.compare("", ""));
        }

        @Test
        void identicalStrings_returnsTrue() {
            assertTrue(strategy.compare("hello world", "hello world"));
        }

        @Test
        void differentStrings_returnsFalse() {
            assertFalse(strategy.compare("hello", "world"));
        }

        @Test
        void caseDifference_returnsFalse() {
            assertFalse(strategy.compare("Hello", "hello"));
        }

        @Test
        void trailingWhitespaceDifference_returnsFalse() {
            assertFalse(strategy.compare("hello ", "hello"));
        }

        @Test
        void leadingWhitespaceDifference_returnsFalse() {
            assertFalse(strategy.compare(" hello", "hello"));
        }

        @Test
        void newlineDifference_returnsFalse() {
            assertFalse(strategy.compare("hello\n", "hello"));
        }

        @Test
        void multilineIdentical_returnsTrue() {
            assertTrue(strategy.compare("line1\nline2\nline3", "line1\nline2\nline3"));
        }

        @Test
        void getStrategyName_returnsExactMatch() {
            assertEquals("Exact Match", strategy.getStrategyName());
        }
    }

    @Nested
    class IgnoreWhitespaceStrategyTest {

        private IgnoreWhitespaceStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new IgnoreWhitespaceStrategy();
        }

        @Test
        void bothNull_returnsTrue() {
            assertTrue(strategy.compare(null, null));
        }

        @Test
        void actualNull_returnsFalse() {
            assertFalse(strategy.compare(null, "hello"));
        }

        @Test
        void expectedNull_returnsFalse() {
            assertFalse(strategy.compare("hello", null));
        }

        @Test
        void bothEmpty_returnsTrue() {
            assertTrue(strategy.compare("", ""));
        }

        @Test
        void identicalStrings_returnsTrue() {
            assertTrue(strategy.compare("helloworld", "helloworld"));
        }

        @Test
        void differingOnlyInSpaces_returnsTrue() {
            assertTrue(strategy.compare("hello world", "helloworld"));
        }

        @Test
        void differingOnlyInTabs_returnsTrue() {
            assertTrue(strategy.compare("hello\tworld", "helloworld"));
        }

        @Test
        void differingOnlyInNewlines_returnsTrue() {
            assertTrue(strategy.compare("hello\nworld", "helloworld"));
        }

        @Test
        void multipleWhitespaceVariants_returnsTrue() {
            assertTrue(strategy.compare("  hello \t world \n", "helloworld"));
        }

        @Test
        void emptyVsWhitespaceOnly_returnsTrue() {
            assertTrue(strategy.compare("", "   "));
        }

        @Test
        void bothWhitespaceOnly_returnsTrue() {
            assertTrue(strategy.compare("   ", "\t\n"));
        }

        @Test
        void differentContent_returnsFalse() {
            assertFalse(strategy.compare("hello", "world"));
        }

        @Test
        void differentContentWithExtraSpaces_returnsFalse() {
            assertFalse(strategy.compare("hel lo", "wor ld"));
        }

        @Test
        void getStrategyName_returnsIgnoreWhitespace() {
            assertEquals("Ignore Whitespace", strategy.getStrategyName());
        }
    }

    @Nested
    class TrimLinesStrategyTest {

        private TrimLinesStrategy strategy;

        @BeforeEach
        void setUp() {
            strategy = new TrimLinesStrategy();
        }

        @Test
        void bothNull_returnsTrue() {
            assertTrue(strategy.compare(null, null));
        }

        @Test
        void actualNull_returnsFalse() {
            assertFalse(strategy.compare(null, "hello"));
        }

        @Test
        void expectedNull_returnsFalse() {
            assertFalse(strategy.compare("hello", null));
        }

        @Test
        void bothEmpty_returnsTrue() {
            assertTrue(strategy.compare("", ""));
        }

        @Test
        void identicalSingleLine_returnsTrue() {
            assertTrue(strategy.compare("hello", "hello"));
        }

        @Test
        void identicalMultiline_returnsTrue() {
            assertTrue(strategy.compare("line1\nline2\nline3", "line1\nline2\nline3"));
        }

        @Test
        void trailingSpacesOnLines_returnsTrue() {
            assertTrue(strategy.compare("hello   \nworld   ", "hello\nworld"));
        }

        @Test
        void leadingSpacesOnLines_returnsTrue() {
            assertTrue(strategy.compare("   hello\n   world", "hello\nworld"));
        }

        @Test
        void leadingAndTrailingSpacesOnLines_returnsTrue() {
            assertTrue(strategy.compare("  hello  \n  world  ", "hello\nworld"));
        }

        @Test
        void windowsLineEndings_returnsTrue() {
            assertTrue(strategy.compare("hello\r\nworld", "hello\nworld"));
        }

        @Test
        void differentLineCount_returnsFalse() {
            assertFalse(strategy.compare("hello\nworld", "hello"));
        }

        @Test
        void differentContent_returnsFalse() {
            assertFalse(strategy.compare("hello\nworld", "hello\nearth"));
        }

        @Test
        void differentContentAfterTrim_returnsFalse() {
            assertFalse(strategy.compare("  hello  \n  world  ", "hello\nearth"));
        }

        @Test
        void getStrategyName_returnsTrimLines() {
            assertEquals("Trim Lines", strategy.getStrategyName());
        }
    }
}
