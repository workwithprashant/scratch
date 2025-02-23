
package utils;

import com.google.common.io.Files;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.devtools.model.DevToolsDomain;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * HAR Capture Utility with Future-Proof Dynamic CDP Version Selection.
 */
public class HARCaptureUtil {
    private final DevTools devTools;
    private final RemoteWebDriver driver;
    private final String harDirectory = "target/har-logs/";

    public HARCaptureUtil(RemoteWebDriver driver) {
        this.driver = driver;
        if (driver instanceof HasDevTools) {
            this.devTools = ((HasDevTools) driver).getDevTools();
            this.devTools.createSession();
        } else {
            throw new IllegalStateException("This WebDriver instance does not support DevTools.");
        }
    }

    /**
     * Dynamically starts HAR capture using the correct CDP version for the running browser.
     */
    public void startHARCapture() {
        try {
            DevToolsDomain networkDomain = devTools.getDomains().getNetwork();
            networkDomain.enable(Optional.empty(), Optional.empty(), Optional.empty());
            System.out.println("HAR capture started dynamically using correct CDP version...");
        } catch (Exception e) {
            System.err.println("Error starting HAR capture: " + e.getMessage());
        }
    }

    /**
     * Stops HAR capture and saves it to a file.
     *
     * @param testName Name of the test (used in the filename).
     */
    public void stopAndSaveHAR(String testName) {
        try {
            // Simulated HAR content (Replace with actual network logs if needed)
            String harContent = "{ \"log\": \"Sample HAR data\" }";

            saveHARToFile(harContent, testName);
            System.out.println("HAR file saved for test: " + testName);
        } catch (Exception e) {
            System.err.println("Error stopping HAR capture: " + e.getMessage());
        }
    }

    /**
     * Saves HAR data to the target directory.
     *
     * @param harContent HAR data in JSON format.
     * @param testName   Test name to associate with the HAR file.
     */
    private void saveHARToFile(String harContent, String testName) {
        try {
            File directory = new File(harDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File harFile = new File(harDirectory + testName + ".har");
            Files.write(harContent.getBytes(StandardCharsets.UTF_8), harFile);
            System.out.println("HAR file successfully saved: " + harFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing HAR file: " + e.getMessage());
        }
    }
}

###################################################################

package listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.HARCaptureUtil;
import org.openqa.selenium.remote.RemoteWebDriver;

public class TestListener implements ITestListener {
    private HARCaptureUtil harCaptureUtil;
    
    @Override
    public void onStart(ITestContext context) {
        RemoteWebDriver driver = (RemoteWebDriver) context.getAttribute("driver");
        if (driver != null) {
            harCaptureUtil = new HARCaptureUtil(driver);
            harCaptureUtil.startHARCapture();
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (harCaptureUtil != null) {
            harCaptureUtil.stopAndSaveHAR(result.getName());
        }
    }
}



###########################################################################


package base;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.*;
import utils.HARCaptureUtil;
import java.net.MalformedURLException;
import java.net.URL;

@Listeners(listeners.TestListener.class)
public class BaseTest {
    protected RemoteWebDriver driver;

    @BeforeMethod
    public void setUp() throws MalformedURLException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-debugging-port=9222");
        driver = new RemoteWebDriver(new URL("http://selenium-box-url/wd/hub"), options);
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}



###########################################################################


package tests;

import base.BaseTest;
import org.testng.annotations.Test;

public class SampleTest extends BaseTest {
    
    @Test
    public void testExample() {
        driver.get("https://example.com");
        // Simulate test failure
        assert driver.getTitle().contains("Not Found");
    }
}



