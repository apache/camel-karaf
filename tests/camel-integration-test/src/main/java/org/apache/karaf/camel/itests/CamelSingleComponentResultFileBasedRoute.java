package org.apache.karaf.camel.itests;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public interface CamelSingleComponentResultFileBasedRoute extends CamelSingleComponentRoute {

    default String getResultFileName() {
        return getTestComponentName();
    }

    default Charset getResultFileCharset() {
        return StandardCharsets.UTF_8;
    }

    default Path assertResultFileExists() {
        Path filePath  = Path.of(getBaseDir(), getResultFileName());
        Awaitility.await().atMost(getTimeoutInSeconds(), TimeUnit.SECONDS)
                .until(() -> Files.exists(filePath));
        assertTrue(Files.exists(filePath));
        return filePath;
    }

    default void assertResultFileContains(String expectedFileContent) throws Exception {
        assertEquals(
                "The content of the result file is not correct",
                expectedFileContent, Files.readString(assertResultFileExists(), getResultFileCharset())
        );
    }

    default void assertResultFileIsSameAs(String expectedResultFileName) throws Exception {
        assertResultFileContains(
                Files.readString(createExpectedResultPath(expectedResultFileName))
        );
    }

    default void assertResultFileIsSameAs(String expectedResultFileName, Charset encoding) throws Exception {
        assertResultFileContains(
                Files.readString(createExpectedResultPath(expectedResultFileName), encoding)
        );
    }

    default Path createExpectedResultPath(String expectedResultFileName) {
        return Path.of(getBaseDir(), "test-classes", expectedResultFileName);
    }

    String getBaseDir();
}
