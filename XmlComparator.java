package com.xml.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

public class XMLComparator {

    private static final Logger logger = LogManager.getLogger(XMLComparator.class);

    /**
     * Compares two XML files while ignoring specific tags.
     *
     * @param shortName1  User-friendly name for first XML file (e.g., "Baseline").
     * @param file1       Path to the first XML file.
     * @param shortName2  User-friendly name for second XML file (e.g., "Modified").
     * @param file2       Path to the second XML file.
     * @param ignoredTags List of XML paths (e.g., "root/nested/tag") to be ignored during comparison.
     * @return List of discrepancies found between the two XMLs.
     */
    public static List<String> compareXMLFiles(String shortName1, Path file1, String shortName2, Path file2, List<String> ignoredTags) {
        List<String> failures = new ArrayList<>();

        try {
            // Parse both XML files
            Document doc1 = parseXML(file1);
            Document doc2 = parseXML(file2);

            if (doc1 == null || doc2 == null) {
                logger.error("One or both XML files could not be parsed.");
                failures.add("Failed to parse XML files.");
                return failures;
            }

            // Convert XML Documents into a map representation
            Map<String, String> xmlMap1 = new LinkedHashMap<>();
            Map<String, String> xmlMap2 = new LinkedHashMap<>();

            buildXMLMap(doc1.getDocumentElement(), "", xmlMap1, ignoredTags);
            buildXMLMap(doc2.getDocumentElement(), "", xmlMap2, ignoredTags);

            failures.addAll(compareMaps(shortName1, xmlMap1, shortName2, xmlMap2));

        } catch (Exception e) {
            logger.error("Exception occurred during XML comparison: ", e);
            failures.add("Exception: " + e.getMessage());
        }

        if (failures.isEmpty()) {
            logger.info("XML comparison successful. No mismatches found.");
        } else {
            logger.warn("XML comparison completed with discrepancies.");
        }

        return failures;
    }

    /**
     * Parses an XML file into a Document object.
     *
     * @param filePath Path to the XML file.
     * @return Parsed Document object or null in case of errors.
     */
    private static Document parseXML(Path filePath) {
        try {
            if (!Files.exists(filePath)) {
                logger.error("File not found: {}", filePath);
                return null;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setIgnoringComments(true);
            dbFactory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(filePath.toFile());
        } catch (Exception e) {
            logger.error("Error parsing XML file: {}", filePath, e);
            return null;
        }
    }

    private static void buildXMLMap(Node node, String path, Map<String, String> xmlMap, List<String> ignoredTags) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String nodePath = path.isEmpty() ? node.getNodeName() : path + "/" + node.getNodeName();

            if (!ignoredTags.contains(nodePath)) {
                if (node.getTextContent() != null && !node.getTextContent().trim().isEmpty()) {
                    xmlMap.put(nodePath, node.getTextContent().trim());
                }
            }

            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                buildXMLMap(children.item(i), nodePath, xmlMap, ignoredTags);
            }
        }
    }

    /**
     * Compares two XML maps and identifies discrepancies, using user-friendly names for each file.
     *
     * @param shortName1 User-friendly name of first XML file.
     * @param map1       XML map representation from file1.
     * @param shortName2 User-friendly name of second XML file.
     * @param map2       XML map representation from file2.
     * @return List of differences found.
     */
    private static List<String> compareMaps(String shortName1, Map<String, String> map1, String shortName2, Map<String, String> map2) {
        List<String> failures = new ArrayList<>();

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(map1.keySet());
        allKeys.addAll(map2.keySet());

        for (String key : allKeys) {
            String value1 = map1.get(key);
            String value2 = map2.get(key);

            if (value1 == null) {
                logger.error("Missing element in {}: {}", shortName1, key);
                failures.add("Missing element in " + shortName1 + ": " + key);
            } else if (value2 == null) {
                logger.error("Missing element in {}: {}", shortName2, key);
                failures.add("Missing element in " + shortName2 + ": " + key);
            } else if (!value1.equals(value2)) {
                logger.error("Mismatch at {} in {}=[{}] vs {}=[{}]", key, shortName1, value1, shortName2, value2);
                failures.add("Mismatch at " + key + ": " + shortName1 + "=[" + value1 + "] " + shortName2 + "=[" + value2 + "]");
            }
        }
        return failures;
    }

    public static void main(String[] args) {
        // Example usage with Paths
        String shortName1 = "Baseline";
        Path filePath1 = Path.of("file1.xml");

        String shortName2 = "Modified";
        Path filePath2 = Path.of("file2.xml");

        // Define ignored tags
        List<String> ignoredTags = Arrays.asList("root/ignoreThis", "root/nested/skipMe");

        // Perform XML comparison
        List<String> results = compareXMLFiles(shortName1, filePath1, shortName2, filePath2, ignoredTags);

        if (!results.isEmpty()) {
            logger.warn("Comparison completed with failures:");
            results.forEach(logger::warn);
        } else {
            logger.info("Comparison successful, no differences found.");
        }
    }
}
