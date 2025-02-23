package utils;

import com.google.common.io.Files;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v119.network.Network;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;

/**
 * Utility class to capture HAR files in small chunks and retain only (N-1) recent files.
 */
public class HARCaptureUtil {
    private final DevTools devTools;
    private final RemoteWebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep only last 4 (N-1) HAR files

    public HARCaptureUtil(RemoteWebDriver driver) {
        this.driver = driver;
        this.devTools = ((ChromeDriver) driver).getDevTools();
        this.devTools.createSession();
    }

    /**
     * Starts HAR capture using Chrome DevTools Protocol.
     */
    public void startHARCapture() {
        try {
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));
            System.out.println("HAR capture started...");
        } catch (Exception e) {
            System.err.println("Error starting HAR capture: " + e.getMessage());
        }
    }

    /**
     * Saves HAR data at intervals and deletes older HAR files.
     */
    public void saveRollingHAR(String testName) {
        try {
            String harContent = "{ \"log\": \"Sample HAR data chunk\" }"; // Mock HAR data
            
            // Save HAR chunk with timestamp
            String fileName = harDirectory + testName + "_" + System.currentTimeMillis() + ".har";
            saveHARToFile(harContent, fileName);

            // Maintain rolling (N-1) HAR files
            harFileNames.add(fileName);
            if (harFileNames.size() > maxHarFiles) {
                String oldestFile = harFileNames.poll();
                if (oldestFile != null) {
                    deleteFile(oldestFile);
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving rolling HAR: " + e.getMessage());
        }
    }

    /**
     * Stops HAR capture and finalizes the HAR file saving.
     */
    public void stopHARCapture() {
        try {
            System.out.println("Finalizing HAR capture...");
        } catch (Exception e) {
            System.err.println("Error stopping HAR capture: " + e.getMessage());
        }
    }

    /**
     * Saves HAR data to a file.
     */
    private void saveHARToFile(String harContent, String filePath) {
        try {
            File directory = new File(harDirectory);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            File harFile = new File(filePath);
            Files.write(harContent.getBytes(StandardCharsets.UTF_8), harFile);
            System.out.println("HAR file saved: " + harFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing HAR file: " + e.getMessage());
        }
    }

    /**
     * Deletes an older HAR file.
     */
    private void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.delete()) {
            System.out.println("Deleted old HAR file: " + filePath);
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

##############################################################################

package tests;

import base.BaseTest;
import org.junit.Test;
import java.util.concurrent.TimeUnit;

public class SampleTest extends BaseTest {

    @Test
    public void testExample() throws InterruptedException {
        driver.get("https://example.com");
        
        // Simulate test execution with periodic HAR capture
        for (int i = 0; i < 10; i++) {
            harCaptureUtil.saveRollingHAR("TestExample");
            TimeUnit.SECONDS.sleep(3); // Simulate test steps
        }

        // Simulate test failure
        assert driver.getTitle().contains("Not Found");
    }
}
################################################################################

package listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.HARCaptureUtil;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestListener implements ITestListener {
    private HARCaptureUtil harCaptureUtil;
    private ScheduledExecutorService scheduler;

    @Override
    public void onStart(ITestContext context) {
        RemoteWebDriver driver = (RemoteWebDriver) context.getAttribute("driver");
        if (driver != null) {
            harCaptureUtil = new HARCaptureUtil(driver);
            harCaptureUtil.startHARCapture();
            scheduler = Executors.newScheduledThreadPool(1);

            // Save HAR every 3 seconds
            scheduler.scheduleAtFixedRate(() -> harCaptureUtil.saveRollingHAR("TestExample"),
                    3, 3, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (harCaptureUtil != null) {
            harCaptureUtil.stopHARCapture();
            scheduler.shutdown(); // Stop periodic HAR capture
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
###############################################################




