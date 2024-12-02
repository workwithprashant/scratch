import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class TableSortValidator {

    /**
     * Capture the table data for all rows and columns.
     *
     * @param table WebElement representing the table.
     * @return List of rows, where each row is a List of column values.
     */
    public static List<List<String>> captureTableData(WebElement table) {
        List<List<String>> tableData = new ArrayList<>();

        // Find all rows in the table body
        List<WebElement> rows = table.findElements(By.xpath(".//tbody/tr"));
        for (WebElement row : rows) {
            List<String> rowData = new ArrayList<>();

            // Find all columns (cells) in the current row
            List<WebElement> cells = row.findElements(By.xpath(".//td"));
            for (WebElement cell : cells) {
                rowData.add(cell.getText().trim());
            }
            tableData.add(rowData);
        }

        return tableData;
    }

    /**
     * Validate that sorting a column updates only the intended column and preserves row data integrity.
     *
     * @param driver      WebDriver instance.
     * @param table       WebElement representing the table.
     * @param header      WebElement representing the column header to sort.
     * @param columnIndex The index of the column to validate sorting for (0-based).
     * @return true if sorting is valid, false otherwise.
     */
    public static boolean validateColumnSorting(WebDriver driver, WebElement table, WebElement header, int columnIndex) {
        // Capture table data before sorting
        List<List<String>> beforeSorting = captureTableData(table);

        // Click the header to sort
        header.click();

        // Capture table data after sorting
        List<List<String>> afterSorting = captureTableData(table);

        // Extract the column to be sorted (before and after)
        List<String> beforeColumn = new ArrayList<>();
        List<String> afterColumn = new ArrayList<>();
        for (List<String> row : beforeSorting) {
            beforeColumn.add(row.get(columnIndex));
        }
        for (List<String> row : afterSorting) {
            afterColumn.add(row.get(columnIndex));
        }

        // Validate the intended column is sorted
        List<String> sortedColumn = new ArrayList<>(beforeColumn);
        sortedColumn.sort(String::compareTo); // Ascending order
        if (!afterColumn.equals(sortedColumn)) {
            System.out.println("Intended column is not sorted correctly.");
            return false;
        }

        // Validate the rest of the columns maintain their row data integrity
        for (int i = 0; i < beforeSorting.size(); i++) {
            for (int j = 0; j < beforeSorting.get(i).size(); j++) {
                if (j != columnIndex && !beforeSorting.get(i).get(j).equals(afterSorting.get(i).get(j))) {
                    System.out.println("Row integrity is broken at row " + i + ", column " + j);
                    return false;
                }
            }
        }

        System.out.println("Sorting is valid, and row data integrity is maintained.");
        return true;
    }

    public static void main(String[] args) {
        WebDriver driver = /* Initialize WebDriver here */;

        // Locate the table and header
        WebElement table = driver.findElement(By.cssSelector("table"));
        WebElement header = table.findElement(By.xpath("//thead/tr/th[normalize-space(text())='Status']"));

        // Validate sorting for the "Status" column (assume columnIndex = 0)
        boolean isValid = validateColumnSorting(driver, table, header, 0);

        if (isValid) {
            System.out.println("Sorting validation passed!");
        } else {
            System.out.println("Sorting validation failed!");
        }

        // Close the browser
        driver.quit();
    }
}


// import org.openqa.selenium.By;
// import org.openqa.selenium.WebDriver;
// import org.openqa.selenium.WebElement;

// import java.util.ArrayList;
// import java.util.List;

// public class TableSortValidator {

//     /**
//      * Capture the table data for all rows and columns.
//      *
//      * @param table WebElement representing the table.
//      * @return List of rows, where each row is a List of column values.
//      */
//     public static List<List<String>> captureTableData(WebElement table) {
//         List<List<String>> tableData = new ArrayList<>();

//         // Find all rows in the table body
//         List<WebElement> rows = table.findElements(By.xpath(".//tbody/tr"));
//         for (WebElement row : rows) {
//             List<String> rowData = new ArrayList<>();

//             // Find all columns (cells) in the current row
//             List<WebElement> cells = row.findElements(By.xpath(".//td"));
//             for (WebElement cell : cells) {
//                 rowData.add(cell.getText().trim());
//             }
//             tableData.add(rowData);
//         }

//         return tableData;
//     }

//     /**
//      * Validate that sorting a column updates only the intended column and preserves row data integrity.
//      *
//      * @param driver      WebDriver instance.
//      * @param table       WebElement representing the table.
//      * @param header      WebElement representing the column header to sort.
//      * @param columnIndex The index of the column to validate sorting for (0-based).
//      * @return true if sorting is valid, false otherwise.
//      */
//     public static boolean validateColumnSorting(WebDriver driver, WebElement table, WebElement header, int columnIndex) {
//         // Capture table data before sorting
//         List<List<String>> beforeSorting = captureTableData(table);

//         // Click the header to sort
//         header.click();

//         // Capture table data after sorting
//         List<List<String>> afterSorting = captureTableData(table);

//         // Extract the column to be sorted (before and after)
//         List<String> beforeColumn = new ArrayList<>();
//         List<String> afterColumn = new ArrayList<>();
//         for (List<String> row : beforeSorting) {
//             beforeColumn.add(row.get(columnIndex));
//         }
//         for (List<String> row : afterSorting) {
//             afterColumn.add(row.get(columnIndex));
//         }

//         // Validate the intended column is sorted
//         List<String> sortedColumn = new ArrayList<>(beforeColumn);
//         sortedColumn.sort(String::compareTo); // Ascending order
//         if (!afterColumn.equals(sortedColumn)) {
//             System.out.println("Intended column is not sorted correctly.");
//             return false;
//         }

//         // Validate the rest of the columns maintain their row data integrity
//         for (int i = 0; i < beforeSorting.size(); i++) {
//             for (int j = 0; j < beforeSorting.get(i).size(); j++) {
//                 if (j != columnIndex && !beforeSorting.get(i).get(j).equals(afterSorting.get(i).get(j))) {
//                     System.out.println("Row integrity is broken at row " + i + ", column " + j);
//                     return false;
//                 }
//             }
//         }

//         System.out.println("Sorting is valid, and row data integrity is maintained.");
//         return true;
//     }

//     public static void main(String[] args) {
//         WebDriver driver = /* Initialize WebDriver here */;

//         // Locate the table and header
//         WebElement table = driver.findElement(By.cssSelector("table"));
//         WebElement header = table.findElement(By.xpath("//thead/tr/th[normalize-space(text())='Status']"));

//         // Validate sorting for the "Status" column (assume columnIndex = 0)
//         boolean isValid = validateColumnSorting(driver, table, header, 0);

//         if (isValid) {
//             System.out.println("Sorting validation passed!");
//         } else {
//             System.out.println("Sorting validation failed!");
//         }

//         // Close the browser
//         driver.quit();
//     }
// }
