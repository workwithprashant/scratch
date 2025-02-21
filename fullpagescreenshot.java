import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.OutputType;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScreenshotUtil {
    private static final Logger LOGGER = Logger.getLogger(ScreenshotUtil.class.getName());

    /**
     * Captures a full-page screenshot as a Base64 string on a remote browser grid.
     * If the screenshot fails, it logs the error and returns an empty string instead of failing the test.
     *
     * @param driver The WebDriver instance (RemoteWebDriver recommended)
     * @return Base64 string of the full-page screenshot or empty string on failure.
     */
    public static String captureFullPageScreenshotBase64(WebDriver driver) {
        try {
            if (!(driver instanceof TakesScreenshot)) {
                LOGGER.warning("Driver does not support taking screenshots.");
                return "";
            }

            // Ensure the driver is RemoteWebDriver for Grid-specific handling
            if (!(driver instanceof RemoteWebDriver)) {
                LOGGER.warning("This method is intended for RemoteWebDriver instances.");
                return "";
            }

            // Get session ID (useful for debugging in Grid logs)
            SessionId sessionId = ((RemoteWebDriver) driver).getSessionId();
            LOGGER.info("Capturing screenshot for session: " + sessionId);

            // Scroll to the top before capturing (ensures full-page capture)
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, 0);");

            // Introduce a short wait for any lazy-loading elements
            driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);

            // Capture screenshot as Base64
            String base64Screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);

            // Reset timeout (optional)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

            LOGGER.info("Screenshot captured successfully for session: " + sessionId);
            return base64Screenshot;

        } catch (TimeoutException e) {
            LOGGER.log(Level.SEVERE, "Screenshot capture timed out.", e);
        } catch (WebDriverException e) {
            LOGGER.log(Level.SEVERE, "WebDriverException while capturing screenshot.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error while capturing screenshot.", e);
        }
        
        // Return an empty string to avoid test failure on screenshot failure
        return "";
    }
}



##############################################################################

WebDriver driver = new RemoteWebDriver(new URL("http://your.selenium.grid/wd/hub"), options);

String base64Screenshot = ScreenshotUtil.captureFullPageScreenshotBase64(driver);
if (!base64Screenshot.isEmpty()) {
    System.out.println("Screenshot captured successfully!");
} else {
    System.out.println("Screenshot capture failed, but test continues.");
}
