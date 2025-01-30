package com.universal.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

/**
 * UniversalComparator compares XML and JSON files while ignoring structure order.
 * - Compares XML elements & attributes.
 * - Compares JSON objects & keys.
 * - Provides structured diff reports.
 * - Properly handles ignored tags as substrings.
 */
public class UniversalComparator {

    private static final Logger logger = LogManager.getLogger(UniversalComparator.class);

    /**
     * Compares two XML files and generates a structured difference report.
     */
    public static DiffResult compareXMLFiles(String shortName1, Path file1, String shortName2, Path file2, List<String> ignoredTags) throws Exception {
        Document doc1 = parseXML(file1);
        Document doc2 = parseXML(file2);

        Map<String, String> xmlMap1 = new TreeMap<>();
        Map<String, String> xmlMap2 = new TreeMap<>();
        Map<String, Map<String, String>> attrMap1 = new TreeMap<>();
        Map<String, Map<String, String>> attrMap2 = new TreeMap<>();

        buildXMLMap(doc1.getDocumentElement(), "", xmlMap1, attrMap1, ignoredTags);
        buildXMLMap(doc2.getDocumentElement(), "", xmlMap2, attrMap2, ignoredTags);

        return compareMaps(shortName1, xmlMap1, attrMap1, shortName2, xmlMap2, attrMap2);
    }

    /**
     * Parses an XML file into a Document object.
     */
    private static Document parseXML(Path filePath) throws Exception {
        if (!Files.exists(filePath)) {
            throw new Exception("File not found: " + filePath);
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(true);
            dbFactory.setIgnoringComments(true);
            dbFactory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(filePath.toFile());
        } catch (Exception e) {
            throw new Exception("Error parsing XML file: " + filePath, e);
        }
    }

    /**
     * Recursively builds a map representation of XML elements and attributes.
     */
    private static void buildXMLMap(Node node, String path, Map<String, String> xmlMap, Map<String, Map<String, String>> attrMap, List<String> ignoredTags) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String nodePath = path.isEmpty() ? node.getNodeName() : path + "/" + node.getNodeName();

            // Skip nodes that contain any ignored tags
            if (!isIgnored(nodePath, ignoredTags)) {
                NodeList children = node.getChildNodes();
                boolean hasChildElements = false;

                // Check if the node has child elements
                for (int i = 0; i < children.getLength(); i++) {
                    if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        hasChildElements = true;
                        break;
                    }
                }

                // Only add leaf node values
                if (!hasChildElements && node.getTextContent() != null && !node.getTextContent().trim().isEmpty()) {
                    xmlMap.put(nodePath, node.getTextContent().trim());
                }

                // Store attributes
                NamedNodeMap attributes = node.getAttributes();
                if (attributes != null && attributes.getLength() > 0) {
                    Map<String, String> attrValues = new TreeMap<>();
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Node attr = attributes.item(i);
                        attrValues.put(attr.getNodeName(), attr.getNodeValue());
                    }
                    attrMap.put(nodePath, attrValues);
                }

                // Recursively process child elements
                for (int i = 0; i < children.getLength(); i++) {
                    buildXMLMap(children.item(i), nodePath, xmlMap, attrMap, ignoredTags);
                }
            }
        }
    }

    /**
     * Checks if a nodePath should be ignored based on ignoredTags list.
     */
    private static boolean isIgnored(String nodePath, List<String> ignoredTags) {
        for (String ignoredTag : ignoredTags) {
            if (nodePath.contains(ignoredTag)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            Path xmlFile1 = Path.of("file1.xml");
            Path xmlFile2 = Path.of("file2.xml");

            // Example ignored tags (will ignore any nodePath containing these)
            List<String> ignoredTags = Arrays.asList("root/ignoreThis", "root/nested/skipMe");

            DiffResult xmlResult = compareXMLFiles("Baseline", xmlFile1, "Modified", xmlFile2, ignoredTags);
            xmlResult.printSummary();

        } catch (Exception e) {
            logger.error("Critical error: {}", e.getMessage(), e);
        }
    }
}
