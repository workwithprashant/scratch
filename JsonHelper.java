package com.mycompany.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A utility class for working with JSON data using Jackson.
 * 
 * <p>Important dependencies (Maven coordinates):
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.fasterxml.jackson.core&lt;/groupId&gt;
 *   &lt;artifactId&gt;jackson-databind&lt;/artifactId&gt;
 *   &lt;version&gt;2.14.0&lt;/version&gt;  &lt;!-- or your chosen version --&gt;
 * &lt;/dependency&gt;
 * </pre>
 * </p>
 */
public final class JsonHelper {
    
    // Reuse a single ObjectMapper for thread-safe read operations.
    // (Thread-safety note: reading with ObjectMapper is thread-safe; 
    // however, modifying settings at runtime is not.)
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Private constructor to prevent instantiation.
    private JsonHelper() {
        // Utility class
    }

    /**
     * Sorts the given JSON array string by the "mtime" field (RFC_1123 format).
     * 
     * @param json      JSON array, e.g.: 
     *                  <pre>[{"name":"fd.log","mtime":"Thu, 30 Jan 2025 15:24:52 GMT"}, ...]</pre>
     * @param ascending True for ascending order, false for descending.
     * @return          List of "name" values sorted by "mtime".
     * @throws IOException If the JSON cannot be parsed.
     */
    public static List<String> sortJsonByMtime(final String json, final boolean ascending) throws IOException {
        
        // Parse JSON into a list of maps
        List<Map<String, Object>> items = parseJsonStringToList(json);

        // Comparator that parses "mtime" in RFC_1123 format
        Comparator<Map<String, Object>> byMtime = (m1, m2) -> {
            ZonedDateTime dt1 = ZonedDateTime.parse(
                    String.valueOf(m1.get("mtime")), DateTimeFormatter.RFC_1123_DATE_TIME);
            ZonedDateTime dt2 = ZonedDateTime.parse(
                    String.valueOf(m2.get("mtime")), DateTimeFormatter.RFC_1123_DATE_TIME);
            return dt1.compareTo(dt2);
        };

        // Sort in ascending order by default
        items.sort(byMtime);
        if (!ascending) {
            Collections.reverse(items);
        }

        // Extract "name" values in sorted order
        return items.stream()
                .map(m -> String.valueOf(m.get("name")))
                .collect(Collectors.toList());
    }

    /**
     * Parses a JSON string into a Map<String, Object>.
     * 
     * @param json JSON string representing an object.
     * @return     A Map of key-value pairs.
     * @throws IOException If the JSON cannot be parsed or is invalid.
     */
    public static Map<String, Object> parseJsonStringToMap(String json) throws IOException {
        return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Parses a JSON string into a List of Map<String, Object>.
     * 
     * @param json JSON string representing an array.
     * @return     A List of Maps where each map corresponds to a JSON object in the array.
     * @throws IOException If the JSON cannot be parsed or is invalid.
     */
    public static List<Map<String, Object>> parseJsonStringToList(String json) throws IOException {
        return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
    }

    /**
     * Checks if the provided string is valid JSON.
     * 
     * @param json The JSON string to validate.
     * @return     True if valid JSON; false otherwise.
     */
    public static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Converts a Java object (Map, List, POJO, etc.) to its JSON string representation.
     * 
     * @param object Any Java object.
     * @return       JSON-formatted string (compact).
     * @throws JsonProcessingException If the object cannot be serialized to JSON.
     */
    public static String convertObjectToJson(Object object) throws JsonProcessingException {
        // Note: By default, ObjectMapper writes JSON in a compact form. 
        // If you want pretty-print JSON:
        // return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        return MAPPER.writeValueAsString(object);
    }

    /**
     * Converts a Java object (Map, List, POJO, etc.) to its pretty-printed JSON string representation.
     * 
     * @param object Any Java object.
     * @return       JSON-formatted string (pretty-printed).
     * @throws JsonProcessingException If the object cannot be serialized to JSON.
     */
    public static String convertObjectToPrettyJson(Object object) throws JsonProcessingException {
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }
    
    /**
     * Example usage / quick test.
     */
    public static void main(String[] args) {
        try {
            String jsonString = """
                [
                  {"name":"fd.log","mtime":"Thu, 30 Jan 2025 15:24:52 GMT"},
                  {"name":"3fd.log","mtime":"Thu, 30 Jan 2025 11:24:52 GMT"},
                  {"name":"4fd.log","mtime":"Thu, 31 Jan 2025 11:24:52 GMT"}
                ]
                """;

            // Sort ascending
            List<String> ascendingNames = sortJsonByMtime(jsonString, true);
            System.out.println("Ascending by mtime: " + ascendingNames);

            // Sort descending
            List<String> descendingNames = sortJsonByMtime(jsonString, false);
            System.out.println("Descending by mtime: " + descendingNames);

            // Validate JSON
            System.out.println("Is valid JSON? " + isValidJson(jsonString));

            // Convert list to JSON
            String listToJson = convertObjectToJson(ascendingNames);
            System.out.println("List to JSON: " + listToJson);

            // Pretty-print
            System.out.println("Pretty JSON: " + convertObjectToPrettyJson(ascendingNames));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
