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

        // Build maps but only compare leaf nodes, not root elements
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

            // Ignore specified tags
            if (!ignoredTags.contains(nodePath)) {
                NodeList children = node.getChildNodes();
                boolean hasChildElements = false;

                // Check if the node has element children
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
     * Compares two JSON files and generates a structured difference report.
     */
    public static DiffResult compareJSONFiles(String shortName1, Path file1, String shortName2, Path file2) throws Exception {
        String json1 = Files.readString(file1);
        String json2 = Files.readString(file2);

        JSONObject jsonObj1 = new JSONObject(json1);
        JSONObject jsonObj2 = new JSONObject(json2);

        DiffResult diffResult = new DiffResult();
        compareJSONObjects(jsonObj1, jsonObj2, "", shortName1, shortName2, diffResult);
        return diffResult;
    }

    /**
     * Recursively compares two JSON objects.
     */
    private static void compareJSONObjects(JSONObject json1, JSONObject json2, String path, String shortName1, String shortName2, DiffResult diffResult) {
        Set<String> keys = new HashSet<>(json1.keySet());
        keys.addAll(json2.keySet());

        for (String key : keys) {
            String fullPath = path.isEmpty() ? key : path + "/" + key;
            if (!json1.has(key)) {
                diffResult.addMissingElement(shortName1, fullPath);
            } else if (!json2.has(key)) {
                diffResult.addMissingElement(shortName2, fullPath);
            } else {
                Object val1 = json1.get(key);
                Object val2 = json2.get(key);

                if (val1 instanceof JSONObject && val2 instanceof JSONObject) {
                    compareJSONObjects((JSONObject) val1, (JSONObject) val2, fullPath, shortName1, shortName2, diffResult);
                } else if (!val1.toString().equals(val2.toString())) {
                    diffResult.addMismatch(fullPath, shortName1, val1.toString(), shortName2, val2.toString());
                }
            }
        }
    }

    /**
     * Compares two maps (used for both XML & JSON).
     */
    private static DiffResult compareMaps(String shortName1, Map<String, String> map1, Map<String, Map<String, String>> attrMap1,
                                          String shortName2, Map<String, String> map2, Map<String, Map<String, String>> attrMap2) {
        DiffResult diffResult = new DiffResult();

        Set<String> allKeys = new HashSet<>(map1.keySet());
        allKeys.addAll(map2.keySet());

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

        return diffResult;
    }

    public static void main(String[] args) {
        try {
            Path xmlFile1 = Path.of("file1.xml");
            Path xmlFile2 = Path.of("file2.xml");
            Path jsonFile1 = Path.of("file1.json");
            Path jsonFile2 = Path.of("file2.json");

            List<String> ignoredTags = Arrays.asList("root/ignoreThis");

            DiffResult xmlResult = compareXMLFiles("Baseline", xmlFile1, "Modified", xmlFile2, ignoredTags);
            xmlResult.printSummary();

            DiffResult jsonResult = compareJSONFiles("Baseline", jsonFile1, "Modified", jsonFile2);
            jsonResult.printSummary();

        } catch (Exception e) {
            logger.error("Critical error: {}", e.getMessage(), e);
        }
    }
}
