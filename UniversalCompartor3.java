package com.universal.comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.*;

/**
 * UniversalComparator compares XML files while handling repeating elements.
 * - Supports XML element & attribute comparison.
 * - Handles unordered structures by sorting elements before comparison.
 * - Supports optional secondary sorting field for more refined ordering.
 */
public class UniversalComparator {

    private static final Logger logger = LogManager.getLogger(UniversalComparator.class);

    /**
     * Compares two XML files while handling multiple repeating elements.
     */
    public static DiffResult compareXMLFiles(String shortName1, Path file1, String shortName2, Path file2, List<String> ignoredTags, String primarySortField, String secondarySortField) throws Exception {
        Document doc1 = parseXML(file1);
        Document doc2 = parseXML(file2);

        Element root1 = doc1.getDocumentElement();
        Element root2 = doc2.getDocumentElement();

        if (!root1.getNodeName().equals(root2.getNodeName())) {
            throw new Exception("Root nodes do not match: " + root1.getNodeName() + " vs " + root2.getNodeName());
        }

        Map<String, List<Map<String, String>>> xmlListMap1 = new HashMap<>();
        Map<String, List<Map<String, String>>> xmlListMap2 = new HashMap<>();

        NodeList children1 = root1.getChildNodes();
        NodeList children2 = root2.getChildNodes();

        for (int i = 0; i < children1.getLength(); i++) {
            buildXMLMap(children1.item(i), "", xmlListMap1, ignoredTags);
        }
        for (int i = 0; i < children2.getLength(); i++) {
            buildXMLMap(children2.item(i), "", xmlListMap2, ignoredTags);
        }

        return compareListMaps(shortName1, xmlListMap1, shortName2, xmlListMap2, primarySortField, secondarySortField);
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
     * Recursively builds a list-based map representation of XML elements.
     */
    private static void buildXMLMap(Node node, String path, Map<String, List<Map<String, String>>> xmlListMap, List<String> ignoredTags) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String nodePath = path.isEmpty() ? node.getNodeName() : path + "/" + node.getNodeName();

            if (!isIgnored(nodePath, ignoredTags)) {
                Map<String, String> elementData = new HashMap<>();
                NodeList children = node.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        elementData.put(child.getNodeName(), child.getTextContent().trim());
                    }
                }

                xmlListMap.putIfAbsent(nodePath, new ArrayList<>());
                xmlListMap.get(nodePath).add(elementData);
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

    /**
     * Compares two XML maps containing lists of repeating elements.
     */
    private static DiffResult compareListMaps(String shortName1, Map<String, List<Map<String, String>>> map1, String shortName2, Map<String, List<Map<String, String>>> map2, String primarySortField, String secondarySortField) {
        DiffResult diffResult = new DiffResult();

        Set<String> allKeys = new HashSet<>(map1.keySet());
        allKeys.addAll(map2.keySet());

        for (String key : allKeys) {
            List<Map<String, String>> list1 = map1.getOrDefault(key, new ArrayList<>());
            List<Map<String, String>> list2 = map2.getOrDefault(key, new ArrayList<>());

            if (list1.isEmpty()) {
                diffResult.addMissingElement(shortName1, key);
            } else if (list2.isEmpty()) {
                diffResult.addMissingElement(shortName2, key);
            } else {
                // Sort using primary and optional secondary field
                list1.sort(Comparator.comparing((Map<String, String> map) -> map.getOrDefault(primarySortField, ""))
                        .thenComparing(map -> map.getOrDefault(secondarySortField, "")));

                list2.sort(Comparator.comparing((Map<String, String> map) -> map.getOrDefault(primarySortField, ""))
                        .thenComparing(map -> map.getOrDefault(secondarySortField, "")));

                for (int i = 0; i < Math.max(list1.size(), list2.size()); i++) {
                    Map<String, String> entry1 = i < list1.size() ? list1.get(i) : null;
                    Map<String, String> entry2 = i < list2.size() ? list2.get(i) : null;

                    if (entry1 == null) {
                        diffResult.addMissingElement(shortName1, key + "[index=" + i + "]");
                    } else if (entry2 == null) {
                        diffResult.addMissingElement(shortName2, key + "[index=" + i + "]");
                    } else {
                        for (String field : entry1.keySet()) {
                            String val1 = entry1.get(field);
                            String val2 = entry2.getOrDefault(field, null);

                            if (val2 == null) {
                                diffResult.addMissingElement(shortName2, key + "/" + field);
                            } else if (!val1.equals(val2)) {
                                diffResult.addMismatch(key + "/" + field, shortName1, val1, shortName2, val2);
                            }
                        }
                    }
                }
            }
        }

        return diffResult;
    }

    public static void main(String[] args) {
        try {
            Path xmlFile1 = Path.of("1.xml");
            Path xmlFile2 = Path.of("2.xml");

            List<String> ignoredTags = Arrays.asList("root/ignoreThis");

            // Using primary sort as "title", and secondary sort as "publish_date"
            DiffResult xmlResult = compareXMLFiles("Baseline", xmlFile1, "Modified", xmlFile2, ignoredTags, "title", "publish_date");
            xmlResult.printSummary();

        } catch (Exception e) {
            logger.error("Critical error: {}", e.getMessage(), e);
        }
    }
}
