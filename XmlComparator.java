package com.xml.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class XMLComparator {

    // Initialize Log4j2 logger for structured logging
    private static final Logger logger = LogManager.getLogger(XMLComparator.class);

    /**
     * Compares two XML files while ignoring specific tags.
     *
     * @param file1       Path to the first XML file.
     * @param file2       Path to the second XML file.
     * @param ignoredTags List of XML paths (e.g., "root/nested/tag") to be ignored during comparison.
     * @return List of discrepancies found between the two XMLs.
     */
    public static List<String> compareXMLFiles(String file1, String file2, List<String> ignoredTags) {
        List<String> failures = new ArrayList<>();

        try {
            // Parse both XML files into Document objects
            Document doc1 = parseXML(file1);
            Document doc2 = parseXML(file2);

            // Handle parsing failures
            if (doc1 == null || doc2 == null) {
                logger.error("One or both XML files could not be parsed.");
                failures.add("Failed to parse XML files.");
                return failures;
            }

            // Convert XML Documents into a map representation for easy comparison
            Map<String, String> xmlMap1 = new LinkedHashMap<>();
            Map<String, String> xmlMap2 = new LinkedHashMap<>();

            // Recursively build XML maps while ignoring specified tags
            buildXMLMap(doc1.getDocumentElement(), "", xmlMap1, ignoredTags);
            buildXMLMap(doc2.getDocumentElement(), "", xmlMap2, ignoredTags);

            // Compare the two XML maps and collect differences
            failures.addAll(compareMaps(xmlMap1, xmlMap2));

        } catch (Exception e) {
            logger.error("Exception occurred during XML comparison: ", e);
            failures.add("Exception: " + e.getMessage());
        }

        // Log final result
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
    private static Document parseXML(String filePath) {
        try {
            File file = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            
            // Ignore whitespace and comments for cleaner comparison
            dbFactory.setNamespaceAware(true);
            dbFactory.setIgnoringComments(true);
            dbFactory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(file);
        } catch (Exception e) {
            logger.error("Error parsing XML file: " + filePath, e);
            return null;
        }
    }

    /**
     * Recursively builds a key-value map representation of an XML document.
     *
     * @param node        Current XML node being processed.
     * @param path        Hierarchical path to the node (e.g., "root/child").
     * @param xmlMap      Map storing XML paths and corresponding text values.
     * @param ignoredTags List of paths to be ignored during processing.
     */
    private static void buildXMLMap(Node node, String path, Map<String, String> xmlMap, List<String> ignoredTags) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            // Construct full hierarchical path for the current node
            String nodePath = path.isEmpty() ? node.getNodeName() : path + "/" + node.getNodeName();

            // Check if this tag should be ignored
            if (!ignoredTags.contains(nodePath)) {
                // Store node value if it's meaningful (not empty)
                if (node.getTextContent() != null && !node.getTextContent().trim().isEmpty()) {
                    xmlMap.put(nodePath, node.getTextContent().trim());
                }
            }

            // Recursively process child nodes
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                buildXMLMap(children.item(i), nodePath, xmlMap, ignoredTags);
            }
        }
    }

    /**
     * Compares two XML maps and identifies discrepancies.
     *
     * @param map1 XML map representation from file1.
     * @param map2 XML map representation from file2.
     * @return List of differences found.
     */
    private static List<String> compareMaps(Map<String, String> map1, Map<String, String> map2) {
        List<String> failures = new ArrayList<>();

        // Merge all keys from both maps to ensure a complete comparison
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(map1.keySet());
        allKeys.addAll(map2.keySet());

        // Iterate through all keys to check for mismatches
        for (String key : allKeys) {
            String value1 = map1.get(key);
            String value2 = map2.get(key);

            // If the key exists in one map but not the other, it's a missing element
            if (value1 == null) {
                logger.error("Missing element in XML1: " + key);
                failures.add("Missing element in XML1: " + key);
            } else if (value2 == null) {
                logger.error("Missing element in XML2: " + key);
                failures.add("Missing element in XML2: " + key);
            }
            // If both keys exist but have different values, it's a content mismatch
            else if (!value1.equals(value2)) {
                logger.error("Mismatch at " + key + ": XML1=[" + value1 + "] XML2=[" + value2 + "]");
                failures.add("Mismatch at " + key + ": XML1=[" + value1 + "] XML2=[" + value2 + "]");
            }
        }
        return failures;
    }

    /**
     * Main method for testing XML comparison.
     */
    public static void main(String[] args) {
        // Define a list of tags to be ignored during comparison (supports nested paths)
        List<String> ignoredTags = Arrays.asList("root/ignoreThis", "root/nested/skipMe");

        // Perform XML comparison
        List<String> results = compareXMLFiles("file1.xml", "file2.xml", ignoredTags);

        // Log comparison results
        if (!results.isEmpty()) {
            logger.warn("Comparison completed with failures:");
            results.forEach(logger::warn);
        } else {
            logger.info("Comparison successful, no differences found.");
        }
    }
}
