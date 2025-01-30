import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    /**
     * Parses an XML file and returns a Document object.
     *
     * @param file XML file to parse
     * @return Document object if parsing is successful, otherwise empty
     */
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

    /**
     * Retrieves the value of the first matching XML tag.
     *
     * @param document XML document
     * @param tagName  Name of the tag
     * @return Value of the tag if found, otherwise empty
     */
    public static Optional<String> getTagValue(Document document, String tagName) {
        NodeList nodeList = document.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return Optional.ofNullable(nodeList.item(0).getTextContent().trim());
        }
        return Optional.empty();
    }

    /**
     * Retrieves all values of a specified XML tag.
     *
     * @param document XML document
     * @param tagName  Name of the tag
     * @return List of tag values
     */
    public static List<String> getAllTagValues(Document document, String tagName) {
        List<String> values = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            values.add(nodeList.item(i).getTextContent().trim());
        }
        return values;
    }

    /**
     * Retrieves the value of a nested tag using an XPath expression.
     *
     * @param document XML document
     * @param xPathExpr XPath expression to locate the tag
     * @return Value of the tag if found, otherwise empty
     */
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
     * Searches multiple XML files for a given nested tag value.
     *
     * @param files List of XML files to search
     * @param xPathExpr XPath expression for the nested tag
     * @param expectedValue Expected value to match
     * @return List of matching files
     */
    public static List<File> findFilesWithMatchingNestedTag(List<File> files, String xPathExpr, String expectedValue) {
        List<File> matchingFiles = new ArrayList<>();
        for (File file : files) {
            parseXmlFile(file).flatMap(doc -> getNestedTagValue(doc, xPathExpr))
                    .filter(value -> value.equals(expectedValue))
                    .ifPresent(value -> matchingFiles.add(file));
        }
        return matchingFiles;
    }

    /**
     * Retrieves all elements by tag name.
     *
     * @param document XML document
     * @param tagName Tag name
     * @return List of elements
     */
    public static List<Element> getElementsByTag(Document document, String tagName) {
        List<Element> elements = new ArrayList<>();
        NodeList nodeList = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    /**
     * Converts an XML document to a string.
     *
     * @param document XML document
     * @return String representation of the XML
     */
    public static String convertXmlToString(Document document) {
        try {
            javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
            java.io.StringWriter writer = new java.io.StringWriter();
            javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(writer);
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (Exception e) {
            System.err.println("Error converting XML to string: " + e.getMessage());
            return "";
        }
    }
public static void main(String[] args) {
        List<File> xmlFiles = Arrays.asList(new File("file1.xml"), new File("file2.xml"));
        String xPathExpression = "//root/parent/child"; // Adjust as per XML structure
        String expectedValue = "TargetValue";

        List<File> matchingFiles = XmlHelper.findFilesWithMatchingNestedTag(xmlFiles, xPathExpression, expectedValue);
        System.out.println("Matching files: " + matchingFiles);
    }
}