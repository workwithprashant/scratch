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
    public boolean validateSorting(String tableLocator, String columnHeaderLocator, int columnIndex, boolean ascending) {
        try {
            // Locate the table and header element
            WebElement table = driver.findElement(By.cssSelector(tableLocator));
            WebElement header = driver.findElement(By.cssSelector(columnHeaderLocator));

            // Click the header to sort
            header.click();

            // Wait for the table to update (add an appropriate wait if necessary)
            WebDriverWait wait = new WebDriverWait(driver, 10);
            wait.until(ExpectedConditions.stalenessOf(table));

            // Re-fetch the table after sorting
            table = driver.findElement(By.cssSelector(tableLocator));

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

    // Example usage for a table with multiple columns
    public void validateAllColumns(String tableLocator, Map<String, Integer> columnHeaderMap) {
        for (Map.Entry<String, Integer> entry : columnHeaderMap.entrySet()) {
            String headerLocator = entry.getKey();
            int columnIndex = entry.getValue();

            System.out.println("Validating sorting for column index: " + columnIndex);

            // Validate ascending
            if (validateSorting(tableLocator, headerLocator, columnIndex, true)) {
                System.out.println("Ascending sort works for column index: " + columnIndex);
            } else {
                System.out.println("Ascending sort failed for column index: " + columnIndex);
            }

            // Validate descending
            if (validateSorting(tableLocator, headerLocator, columnIndex, false)) {
                System.out.println("Descending sort works for column index: " + columnIndex);
            } else {
                System.out.println("Descending sort failed for column index: " + columnIndex);
            }
        }
    }

    public static void main(String[] args) {
        // Example usage
        WebDriver driver = // Initialize WebDriver here
        TableSortValidator validator = new TableSortValidator(driver);

        // Table locator
        String tableLocator = "table"; // Adjust this to match your table locator

        // Column headers locators and indices (Adjust locators to match your headers)
        Map<String, Integer> columnHeaderMap = new HashMap<>();
        columnHeaderMap.put("th:nth-child(1)", 1); // Header locator and column index
        columnHeaderMap.put("th:nth-child(2)", 2);
        columnHeaderMap.put("th:nth-child(3)", 3);

        // Validate sorting for all columns
        validator.validateAllColumns(tableLocator, columnHeaderMap);

        // Close the driver
        driver.quit();
    }
}
