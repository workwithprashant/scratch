package com.mycompany.automation.utilities;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A utility class to click an element and wait for the page content size to change.
 */
public class PageUpdateSizeUtility {

    /**
     * Clicks the element specified by clickableLocator, then waits until the
     * page source size changes.
     * 
     * @param driver           the WebDriver instance
     * @param clickableLocator locator for the element you want to click
     * @param waitDuration     maximum wait time (e.g. Duration.ofSeconds(10))
     * @return true if the page size changed within the wait duration; false otherwise
     */
    public static boolean clickAndWaitForSizeChange(
            WebDriver driver,
            By clickableLocator,
            Duration waitDuration
    ) {
        WebDriverWait wait = new WebDriverWait(driver, waitDuration);

        try {
            // 1. Ensure the clickable element is ready
            WebElement clickableElement = wait.until(
                    ExpectedConditions.elementToBeClickable(clickableLocator)
            );

            // 2. Get the current page source length (size)
            int oldPageSize = driver.getPageSource().length();

            // 3. Click the element
            clickableElement.click();

            // 4. Wait until the page source size changes
            Boolean isSizeChanged = wait.until((ExpectedCondition<Boolean>) d -> {
                int newPageSize = d.getPageSource().length();
                return newPageSize != oldPageSize;
            });

            // If we got here, it means the page size changed.
            return isSizeChanged != null && isSizeChanged;
        } catch (TimeoutException e) {
            // If we time out, assume the page did not change
            return false;
        }
    }
}
