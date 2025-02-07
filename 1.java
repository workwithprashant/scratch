package com.example.allure;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public class DisabledReasonListener implements IInvokedMethodListener {

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        // no-op
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        if (testResult.getStatus() == ITestResult.SKIP 
         || testResult.getStatus() == ITestResult.CREATED) {
            // In some versions, disabled tests might not get set to SKIP,
            // but they might appear as "CREATED" or something else internally.

            // If we detect that the method is actually disabled
            if (!method.getTestMethod().getEnabled()) {
                testResult.setThrowable(new RuntimeException(
                    "Disabled: This test is @Test(enabled=false)"
                ));
            }
        }
    }
}





package com.example.allure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class UnknownStatusRemover {

    private static final String ALLURE_RESULTS_DIR = "target/allure-results";
    private static final String TEST_RESULT_PREFIX = "test-result-";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Adjust this set to whatever substring(s) you expect in the JSON
     * for @Test(enabled=false) tests. 
     * E.g., "Disabled: This test is @Test(enabled=false)" 
     * or just "@Test(enabled=false)" if that text appears directly.
     */
    private static final Set<String> DISABLED_SUBSTRINGS = new HashSet<>(
        Arrays.asList("@Test(enabled=false)", "Disabled: This test is @Test(enabled=false)")
    );

    public static void main(String[] args) {
        UnknownStatusRemover remover = new UnknownStatusRemover();
        remover.removeUnknownDisabledTests(ALLURE_RESULTS_DIR);
    }

    /**
     * Removes "test-result-*.json" files that have "status": "unknown"
     * AND contain one of our "DISABLED_SUBSTRINGS" in statusDetails.
     */
    public void removeUnknownDisabledTests(String resultsDirPath) {
        Path resultsDir = Paths.get(resultsDirPath);
        if (!Files.exists(resultsDir) || !Files.isDirectory(resultsDir)) {
            System.out.println("[UnknownStatusRemover] Directory " 
                    + resultsDirPath + " does not exist or is not a directory.");
            return;
        }

        try (Stream<Path> filePaths = Files.list(resultsDir)) {
            filePaths
                .filter(this::isTestResultJsonFile)
                .forEach(this::processTestResultFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isTestResultJsonFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.startsWith(TEST_RESULT_PREFIX) && fileName.endsWith(".json");
    }

    private void processTestResultFile(Path path) {
        try {
            JsonNode root = MAPPER.readTree(path.toFile());
            String status = getText(root, "status");

            if ("unknown".equalsIgnoreCase(status)) {
                // Check statusDetails for a unique signature
                JsonNode detailsNode = root.get("statusDetails");
                if (detailsNode != null) {
                    String message = getText(detailsNode, "message");
                    String trace   = getText(detailsNode, "trace");

                    // If message or trace contains known disabled substring, remove the file
                    if (containsDisabledSubstring(message) || containsDisabledSubstring(trace)) {
                        Files.deleteIfExists(path);
                        System.out.println("[UnknownStatusRemover] Deleted file for @Test(enabled=false): " + path);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[UnknownStatusRemover] Could not parse file: " + path);
            e.printStackTrace();
        }
    }

    private String getText(JsonNode node, String fieldName) {
        if (node == null) return "";
        JsonNode valueNode = node.get(fieldName);
        return valueNode == null ? "" : valueNode.asText("");
    }

    private boolean containsDisabledSubstring(String text) {
        for (String token : DISABLED_SUBSTRINGS) {
            if (text != null && text.contains(token)) {
                return true;
            }
        }
        return false;
    }
}










<build>
  <plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>exec-maven-plugin</artifactId>
      <version>3.1.0</version>
      <executions>
        <execution>
          <id>remove-disabled-tests</id>
          <phase>verify</phase>
          <goals>
            <goal>java</goal>
          </goals>
          <configuration>
            <mainClass>com.example.allure.UnknownStatusRemover</mainClass>
          </configuration>
        </execution>
      </executions>
    </plugin>

    <plugin>
      <groupId>io.qameta.allure</groupId>
      <artifactId>allure-maven</artifactId>
      <version>2.11.0</version>
      <executions>
        <execution>
          <id>allure-report</id>
          <phase>verify</phase>
          <goals>
            <goal>aggregate</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>













