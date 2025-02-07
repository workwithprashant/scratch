package com.example.allure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Post-processes Allure result JSON files by removing tests that are:
 *   1) Marked as "unknown" (or "skipped") due to @Test(enabled=false)
 *   2) Do NOT intersect with user-requested groups (-Dgroups=...)
 */
public class DisabledTestRemoverByGroups {

    // The folder where Allure saves test result files, per official docs
    private static final String ALLURE_RESULTS_DIR = "target/allure-results";

    // Substrings that identify a @Test(enabled=false) skip reason in "statusDetails.message" or "trace".
    // Adjust these to match your actual environment.
    private static final List<String> DISABLED_SUBSTRINGS = Arrays.asList(
        "@Test(enabled=false)",
        "Disabled: This test is @Test(enabled=false)"
    );

    // Status used by Allure for disabled tests. Often "unknown", but can be "skipped".
    private static final String DISABLED_STATUS = "unknown";

    // System property where the user can pass -Dgroups=group1,group2
    private static final String GROUPS_PROPERTY = "groups";

    private final Set<String> requestedGroups;  // The user-requested groups
    private final ObjectMapper mapper;

    public DisabledTestRemoverByGroups() {
        this.mapper = new ObjectMapper();

        // Parse -Dgroups into a set
        String groupsParam = System.getProperty(GROUPS_PROPERTY, "").trim();
        if (!groupsParam.isEmpty()) {
            this.requestedGroups = new HashSet<>(Arrays.asList(groupsParam.split(",")));
        } else {
            this.requestedGroups = Collections.emptySet();
        }
    }

    public static void main(String[] args) {
        DisabledTestRemoverByGroups remover = new DisabledTestRemoverByGroups();
        remover.removeTestsWithoutGroupIntersection(ALLURE_RESULTS_DIR);
    }

    /**
     * Removes Allure result files that:
     *  - have status "unknown" (or whatever DISABLED_STATUS is set to),
     *  - contain a skip reason for @Test(enabled=false),
     *  - and do NOT intersect with the user-requested groups.
     */
    public void removeTestsWithoutGroupIntersection(String resultsDirPath) {
        Path resultsDir = Paths.get(resultsDirPath);
        if (!Files.exists(resultsDir) || !Files.isDirectory(resultsDir)) {
            System.out.println("[DisabledTestRemoverByGroups] Directory not found: " + resultsDirPath);
            return;
        }

        try (Stream<Path> paths = Files.list(resultsDir)) {
            paths
                // Optionally exclude known non-test files like categories.json, executor.json, etc.
                .filter(this::isLikelyAllureTestResult)
                .forEach(this::processTestResultFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isLikelyAllureTestResult(Path path) {
        String fname = path.getFileName().toString().toLowerCase(Locale.ROOT);

        // Exclude some known Allure config files if needed:
        if (fname.equals("categories.json") || fname.equals("executor.json")) {
            return false;
        }
        // Otherwise check if it's a .json file (Allure docs recommend .json)
        return fname.endsWith(".json");
    }

    private void processTestResultFile(Path filePath) {
        try {
            JsonNode root = mapper.readTree(filePath.toFile());
            
            // Per official doc: "status" is a top-level field
            String status = getText(root, "status");
            if (DISABLED_STATUS.equalsIgnoreCase(status)) {
                // Then check statusDetails for the skip reason
                JsonNode details = root.get("statusDetails");
                if (details != null) {
                    String message = getText(details, "message");
                    String trace   = getText(details, "trace");

                    boolean isDisabledTest = containsDisabledSubstring(message)
                                          || containsDisabledSubstring(trace);

                    if (isDisabledTest) {
                        // Check the test's groups from "labels"
                        Set<String> testGroups = extractTestGroups(root);

                        // If there's no intersection with requested groups, remove file
                        if (!hasIntersection(testGroups, requestedGroups)) {
                            Files.deleteIfExists(filePath);
                            System.out.println("[DisabledTestRemoverByGroups] Deleted file: " 
                                + filePath + " (disabled test, no group intersection)");
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[DisabledTestRemoverByGroups] Could not parse file: " + filePath);
            e.printStackTrace();
        }
    }

    /**
     * Extract groups from Allure "labels" array:
     *   "labels": [ { "name": "tag", "value": "groupA" }, ... ]
     */
    private Set<String> extractTestGroups(JsonNode root) {
        Set<String> groups = new HashSet<>();
        JsonNode labelsNode = root.get("labels");
        if (labelsNode != null && labelsNode.isArray()) {
            for (JsonNode label : labelsNode) {
                String name  = getText(label, "name");
                String value = getText(label, "value");
                // Allure docs: TestNG groups typically appear as { name: "tag", value: "someGroup" }
                if ("tag".equalsIgnoreCase(name)) {
                    groups.add(value);
                }
            }
        }
        return groups;
    }

    private boolean hasIntersection(Set<String> set1, Set<String> set2) {
        for (String s : set1) {
            if (set2.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDisabledSubstring(String text) {
        if (text == null) return false;
        for (String disabledMarker : DISABLED_SUBSTRINGS) {
            if (text.contains(disabledMarker)) {
                return true;
            }
        }
        return false;
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return "";
        JsonNode val = node.get(field);
        return val == null ? "" : val.asText("");
    }
}





















<build>
  <plugins>
    <!-- Run our Java post-processor in the verify phase -->
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
            <mainClass>com.example.allure.DisabledTestRemoverByGroups</mainClass>
          </configuration>
        </execution>
      </executions>
    </plugin>

    <!-- Then generate Allure's final HTML report -->
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
