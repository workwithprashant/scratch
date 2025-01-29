package com.xml.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

/**
 * XMLComparator is a utility class that compares two XML files and provides
 * a detailed difference report, including mismatched elements, missing elements,
 * and attribute differences.
 */
public class XMLComparator {

    private static final Logger logger = LogManager.getLogger(XMLComparator.class);

    /**
     * Compares two XML files while ignoring specific tags and treating structure as unordered.
     *
     * @param shortName1  A user-friendly name for the first XML file (e.g., "Baseline").
     * @param file1       The path to the first XML file.
     * @param shortName2  A user-friendly name for the second XML file (e.g., "Modified").
     * @param file2       The path to the second XML file.
     * @param ignoredTags A list of XML paths to be ignored during comparison.
     * @return A detailed difference report containing mismatches, missing elements, and attribute differences.
     * @throws Exception If XML parsing fails or the files cannot be read.
     */
    public static DiffResult compareXMLFiles(String shortName1, Path file1, String shortName2, Path file2, List<String> ignoredTags) throws Exception {
        // Parse XML files
        Document doc1 = parseXML(file1);
        Document doc2 = parseXML(file2);

        // Maps to store element values and attributes
        Map<String, String> xmlMap1 = new TreeMap<>();
        Map<String, String> xmlMap2 = new TreeMap<>();
        Map<String, Map<String, String>> attrMap1 = new TreeMap<>();
        Map<String, Map<String, String>> attrMap2 = new TreeMap<>();

        // Build XML maps including attributes
        buildXMLMap(doc1.getDocumentElement(), "", xmlMap1, attrMap1, ignoredTags);
        buildXMLMap(doc2.getDocumentElement(), "", xmlMap2, attrMap2, ignoredTags);

        // Perform comparison and return the result
        return compareMaps(shortName1, xmlMap1, attrMap1, shortName2, xmlMap2, attrMap2);
    }

    /**
     * Parses an XML file into a Document object.
     *
     * @param filePath Path to the XML file.
     * @return Parsed Document object.
     * @throws Exception If the file does not exist or cannot be parsed.
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
     *
     * @param node        The current XML node being processed.
     * @param path        The hierarchical path of the node (e.g., "root/child").
     * @param xmlMap      A map storing element paths and corresponding text values.
     * @param attrMap     A map storing element paths and their attributes.
     * @param ignoredTags A list of tags to be ignored during processing.
     */
    private static void buildXMLMap(Node node, String path, Map<String, String> xmlMap, Map<String, Map<String, String>> attrMap, List<String> ignoredTags) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String nodePath = path.isEmpty() ? node.getNodeName() : path + "/" + node.getNodeName();

            // Ignore specified tags
            if (!ignoredTags.contains(nodePath)) {
                if (node.getTextContent() != null && !node.getTextContent().trim().isEmpty()) {
                    xmlMap.put(nodePath, node.getTextContent().trim());
                }

                // Store attributes of the current node
                NamedNodeMap attributes = node.getAttributes();
                if (attributes != null && attributes.getLength() > 0) {
                    Map<String, String> attrValues = new TreeMap<>();
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Node attr = attributes.item(i);
                        attrValues.put(attr.getNodeName(), attr.getNodeValue());
                    }
                    attrMap.put(nodePath, attrValues);
                }
            }

            // Recursively process child nodes
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                buildXMLMap(children.item(i), nodePath, xmlMap, attrMap, ignoredTags);
            }
        }
    }

    /**
     * Compares two XML maps in an order-independent manner and returns a detailed diff report.
     */
    private static DiffResult compareMaps(String shortName1, Map<String, String> map1, Map<String, Map<String, String>> attrMap1,
                                          String shortName2, Map<String, String> map2, Map<String, Map<String, String>> attrMap2) {
        DiffResult diffResult = new DiffResult();

        Set<String> allKeys = new HashSet<>(map1.keySet());
        allKeys.addAll(map2.keySet());

        // Compare element values
        for (String key : allKeys) {
            String value1 = map1.get(key);
            String value2 = map2.get(key);

            if (value1 == null) {
                diffResult.addMissingElement(shortName1, key);
            } else if (value2 == null) {
                diffResult.addMissingElement(shortName2, key);
            } else if (!value1.equals(value2)) {
                diffResult.addMismatch(key, shortName1, value1, shortName2, value2);
            }
        }

        // Compare attributes
        for (String key : allKeys) {
            Map<String, String> attr1 = attrMap1.getOrDefault(key, new HashMap<>());
            Map<String, String> attr2 = attrMap2.getOrDefault(key, new HashMap<>());

            Set<String> allAttrKeys = new HashSet<>(attr1.keySet());
            allAttrKeys.addAll(attr2.keySet());

            for (String attrKey : allAttrKeys) {
                String attrVal1 = attr1.get(attrKey);
                String attrVal2 = attr2.get(attrKey);

                if (attrVal1 == null) {
                    diffResult.addMissingAttribute(shortName1, key, attrKey);
                } else if (attrVal2 == null) {
                    diffResult.addMissingAttribute(shortName2, key, attrKey);
                } else if (!attrVal1.equals(attrVal2)) {
                    diffResult.addAttributeMismatch(key, attrKey, shortName1, attrVal1, shortName2, attrVal2);
                }
            }
        }

        return diffResult;
    }

    public static void main(String[] args) {
        try {
            Path filePath1 = Path.of("file1.xml");
            Path filePath2 = Path.of("file2.xml");
            List<String> ignoredTags = Arrays.asList("root/ignoreThis", "root/nested/skipMe");

            DiffResult diffResult = compareXMLFiles("Baseline", filePath1, "Modified", filePath2, ignoredTags);
            diffResult.printSummary();

        } catch (Exception e) {
            logger.error("Critical error: {}", e.getMessage(), e);
        }
    }
}

/**
 * The `DiffResult` class is responsible for storing and reporting XML differences,
 * including:
 * - Element value mismatches
 * - Missing elements in either XML file
 * - Missing attributes in either XML file
 * - Attribute value mismatches
 *
 * This class provides structured methods to store mismatches and logs the results.
 */
public class DiffResult {

    private static final Logger logger = LogManager.getLogger(DiffResult.class);

    // Stores element value mismatches
    private final List<String> mismatches = new ArrayList<>();

    // Stores missing elements in either XML file
    private final List<String> missingElements = new ArrayList<>();

    // Stores missing attributes in either XML file
    private final List<String> missingAttributes = new ArrayList<>();

    /**
     * Adds a mismatch when element values differ between the two XML files.
     *
     * @param key        The XML element path where the mismatch occurs.
     * @param shortName1 The name assigned to the first XML file (e.g., "Baseline").
     * @param value1     The value from the first XML file.
     * @param shortName2 The name assigned to the second XML file (e.g., "Modified").
     * @param value2     The value from the second XML file.
     */
    public void addMismatch(String key, String shortName1, String value1, String shortName2, String value2) {
        String message = "Mismatch at " + key + ": " + shortName1 + "=[" + value1 + "] " + shortName2 + "=[" + value2 + "]";
        mismatches.add(message);
    }

    /**
     * Adds a missing element entry when an XML element is found in one file but not the other.
     *
     * @param shortName The name assigned to the XML file where the element is missing.
     * @param key       The XML element path that is missing.
     */
    public void addMissingElement(String shortName, String key) {
        String message = "Missing element in " + shortName + ": " + key;
        missingElements.add(message);
    }

    /**
     * Adds a missing attribute entry when an attribute is found in one XML file but not the other.
     *
     * @param shortName The name assigned to the XML file where the attribute is missing.
     * @param key       The XML element path where the attribute is missing.
     * @param attribute The attribute name that is missing.
     */
    public void addMissingAttribute(String shortName, String key, String attribute) {
        String message = "Missing attribute '" + attribute + "' in " + shortName + " at element: " + key;
        missingAttributes.add(message);
    }

    /**
     * Adds an attribute mismatch entry when attribute values differ between the two XML files.
     *
     * @param key        The XML element path where the attribute mismatch occurs.
     * @param attribute  The name of the attribute that differs.
     * @param shortName1 The name assigned to the first XML file.
     * @param value1     The attribute value from the first XML file.
     * @param shortName2 The name assigned to the second XML file.
     * @param value2     The attribute value from the second XML file.
     */
    public void addAttributeMismatch(String key, String attribute, String shortName1, String value1, String shortName2, String value2) {
        String message = "Mismatch in attribute '" + attribute + "' at " + key + ": " 
                + shortName1 + "=[" + value1 + "] " 
                + shortName2 + "=[" + value2 + "]";
        mismatches.add(message);
    }

    /**
     * Prints a summary of all mismatches, missing elements, and missing attributes.
     * - Logs errors for each detected issue.
     * - If no mismatches are found, logs a success message.
     */
    public void printSummary() {
        // Log mismatches in element values
        for (String mismatch : mismatches) {
            logger.error(mismatch);
        }

        // Log missing elements
        for (String missing : missingElements) {
            logger.error(missing);
        }

        // Log missing attributes
        for (String missingAttr : missingAttributes) {
            logger.error(missingAttr);
        }

        // If no issues were found, log success
        if (mismatches.isEmpty() && missingElements.isEmpty() && missingAttributes.isEmpty()) {
            logger.info("XML comparison successful. No mismatches found.");
        }
    }

    /**
     * Checks if any mismatches, missing elements, or missing attributes exist.
     *
     * @return `true` if differences exist, `false` otherwise.
     */
    public boolean hasDifferences() {
        return !mismatches.isEmpty() || !missingElements.isEmpty() || !missingAttributes.isEmpty();
    }

    /**
     * Gets all recorded mismatches in element values.
     *
     * @return A list of element value mismatches.
     */
    public List<String> getMismatches() {
        return new ArrayList<>(mismatches);
    }

    /**
     * Gets all recorded missing elements.
     *
     * @return A list of missing elements.
     */
    public List<String> getMissingElements() {
        return new ArrayList<>(missingElements);
    }

    /**
     * Gets all recorded missing attributes.
     *
     * @return A list of missing attributes.
     */
    public List<String> getMissingAttributes() {
        return new ArrayList<>(missingAttributes);
    }
}
