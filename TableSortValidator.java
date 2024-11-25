import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.util.*;
import java.util.stream.Collectors;

public class TableSortValidator {

    WebDriver driver;

    public TableSortValidator(WebDriver driver) {
        this.driver = driver;
    }

    // Generic method to validate sorting for a column
    public boolean validateSorting(String tableLocator, String columnHeaderValue, boolean ascending) {
        try {
            // Locate the table
            WebElement table = driver.findElement(By.cssSelector(tableLocator));

            // Locate the column header dynamically by its text value
            WebElement header = table.findElement(By.xpath("//thead/tr/th[normalize-space(text())='" + columnHeaderValue + "']"));

            // Click the header to sort
            header.click();

            // Wait for the table to update (add an appropriate wait if necessary)
            WebDriverWait wait = new WebDriverWait(driver, 10);
            wait.until(ExpectedConditions.stalenessOf(table));

            // Re-fetch the table after sorting
            table = driver.findElement(By.cssSelector(tableLocator));

            // Locate the column index dynamically based on the header
            int columnIndex = getColumnIndexByHeaderValue(table, columnHeaderValue);

            // Get all values from the specific column
            List<WebElement> columnValues = table.findElements(By.xpath("//tbody/tr/td[" + columnIndex + "]"));
            List<String> actualValues = columnValues.stream()
                    .map(WebElement::getText)
                    .collect(Collectors.toList());

            // Create a sorted copy for comparison
            List<String> sortedValues = new ArrayList<>(actualValues);
            if (ascending) {
                sortedValues.sort(String::compareTo);
            } else {
                sortedValues.sort(Collections.reverseOrder());
            }

            // Compare the actual values with the sorted values
            return actualValues.equals(sortedValues);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method to determine the column index dynamically by header value
    private int getColumnIndexByHeaderValue(WebElement table, String columnHeaderValue) {
        List<WebElement> headers = table.findElements(By.xpath("//thead/tr/th"));
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).getText().trim().equalsIgnoreCase(columnHeaderValue.trim())) {
                return i + 1; // XPath column indices start at 1
            }
        }
        throw new NoSuchElementException("Header with value '" + columnHeaderValue + "' not found in the table");
    }

    // Method to validate only parameterized columns
    public void validateSpecificColumns(String tableLocator, List<String> columnHeaderValues) {
        for (String headerValue : columnHeaderValues) {
            System.out.println("Validating sorting for column: " + headerValue);

            // Validate ascending
            if (validateSorting(tableLocator, headerValue, true)) {
                System.out.println("Ascending sort works for column: " + headerValue);
            } else {
                System.out.println("Ascending sort failed for column: " + headerValue);
            }

            // Validate descending
            if (validateSorting(tableLocator, headerValue, false)) {
                System.out.println("Descending sort works for column: " + headerValue);
            } else {
                System.out.println("Descending sort failed for column: " + headerValue);
            }
        }
    }

    public static void main(String[] args) {
        // Example usage
        WebDriver driver = // Initialize WebDriver here
        TableSortValidator validator = new TableSortValidator(driver);

        // Table locator
        String tableLocator = "table"; // Adjust this to match your table locator

        // List of column header values to validate
        List<String> columnHeaderValues = Arrays.asList(
                "Status",     // Example header values based on your table
                "Issuer",
                "Last Updated"
        );

        // Validate sorting for the specified columns
        validator.validateSpecificColumns(tableLocator, columnHeaderValues);

        // Close the driver
        driver.quit();
    }
}
