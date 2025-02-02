import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class XmlHelper {

    public enum Operation {
        AND, OR
    }

    public static class Condition {
        String xPathExpr;
        String expectedValue;
        Operation operation;

        public Condition(String xPathExpr, String expectedValue, Operation operation) {
            this.xPathExpr = xPathExpr;
            this.expectedValue = expectedValue;
            this.operation = operation;
        }
    }

    public static Optional<Document> parseXmlFile(File file) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return Optional.of(builder.parse(file));
        } catch (Exception e) {
            System.err.println("Error parsing XML file: " + file.getName() + " - " + e.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<String> getNestedTagValue(Document document, String xPathExpr) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xPath.compile(xPathExpr);
            return Optional.ofNullable((String) expr.evaluate(document, XPathConstants.STRING)).filter(s -> !s.isEmpty());
        } catch (Exception e) {
            System.err.println("XPath evaluation error: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Searches multiple XML files for multiple matching nested tag values.
     *
     * @param files      List of XML files to search
     * @param conditions List of conditions with XPath expressions, expected values, and operations
     * @return List of matching files
     */
    public static List<File> findFilesWithMatchingNestedTags(List<File> files, List<Condition> conditions) {
        List<File> matchingFiles = new ArrayList<>();

        for (File file : files) {
            Optional<Document> optionalDocument = parseXmlFile(file);

            if (optionalDocument.isPresent()) {
                Document document = optionalDocument.get();
                boolean result = evaluateConditions(document, conditions);

                if (result) {
                    matchingFiles.add(file);
                }
            }
        }
        return matchingFiles;
    }

    /**
     * Evaluates multiple conditions on a given XML document.
     *
     * @param document   XML document
     * @param conditions List of conditions
     * @return true if the conditions satisfy the logical operation
     */
    private static boolean evaluateConditions(Document document, List<Condition> conditions) {
        boolean result = false;
        boolean firstCondition = true;

        for (Condition condition : conditions) {
            boolean currentConditionResult = getNestedTagValue(document, condition.xPathExpr)
                    .map(value -> value.equals(condition.expectedValue))
                    .orElse(false);

            if (firstCondition) {
                result = currentConditionResult;
                firstCondition = false;
            } else {
                if (condition.operation == Operation.AND) {
                    result = result && currentConditionResult;
                } else if (condition.operation == Operation.OR) {
                    result = result || currentConditionResult;
                }
            }
        }

        return result;
    }
}
