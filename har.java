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
 * Utility class to capture HAR files in small chunks, splitting based on size, and retaining only (N-1) latest files.
 */
public class HARCaptureUtil {
    private final DevTools devTools;
    private final RemoteWebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep only last 4 HAR files
    private final long maxHarSizeBytes = 2 * 1024 * 1024; // 2MB per file

    private String currentHarFile;
    private StringBuilder harBuffer = new StringBuilder();

    public HARCaptureUtil(RemoteWebDriver driver) {
        this.driver = driver;
        this.devTools = ((ChromeDriver) driver).getDevTools();
        this.devTools.createSession();
        this.currentHarFile = generateHarFileName();
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
     * Captures HAR data incrementally and splits files based on size.
     */
    public void saveRollingHAR(String testName, String harChunk) {
        try {
            // Append new HAR data
            harBuffer.append(harChunk).append("\n");

            // Check HAR size
            if (harBuffer.length() >= maxHarSizeBytes) {
                flushHARFile();
            }
        } catch (Exception e) {
            System.err.println("Error saving rolling HAR: " + e.getMessage());
        }
    }

    /**
     * Stops HAR capture and flushes the final HAR file.
     */
    public void stopHARCapture() {
        flushHARFile();
        System.out.println("Finalizing HAR capture...");
    }

    /**
     * Writes the buffered HAR data to a file and starts a new one.
     */
    private void flushHARFile() {
        if (harBuffer.length() == 0) return;

        try {
            saveHARToFile(harBuffer.toString(), currentHarFile);
            harFileNames.add(currentHarFile);
            if (harFileNames.size() > maxHarFiles) {
                deleteFile(harFileNames.poll());
            }
        } catch (IOException e) {
            System.err.println("Error writing HAR file: " + e.getMessage());
        }

        // Reset buffer and generate a new HAR file
        harBuffer = new StringBuilder();
        currentHarFile = generateHarFileName();
    }

    /**
     * Generates a new HAR file name.
     */
    private String generateHarFileName() {
        return harDirectory + "HAR_" + System.currentTimeMillis() + ".har";
    }

    /**
     * Saves HAR data to a file.
     */
    private void saveHARToFile(String harContent, String filePath) throws IOException {
        File directory = new File(harDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File harFile = new File(filePath);
        Files.write(harContent.getBytes(StandardCharsets.UTF_8), harFile);
        System.out.println("HAR file saved: " + harFile.getAbsolutePath());
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


########

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
            harCaptureUtil.saveRollingHAR("TestExample", "{ \"log\": \"Sample HAR chunk " + i + "\" }");
            TimeUnit.SECONDS.sleep(2); // Simulate delay
        }

        // Simulate test failure
        assert driver.getTitle().contains("Not Found");
    }
}

####################################################


package listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.HARCaptureUtil;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TestNG Listener for capturing HAR files in a rolling manner independently of test execution.
 */
public class TestListener implements ITestListener {
    private HARCaptureUtil harCaptureUtil;
    private ScheduledExecutorService harScheduler;

    @Override
    public void onStart(ITestContext context) {
        RemoteWebDriver driver = (RemoteWebDriver) context.getAttribute("driver");

        if (driver != null) {
            harCaptureUtil = new HARCaptureUtil(driver);
            harCaptureUtil.startHARCapture();

            // Start background HAR saving every 3 seconds
            harScheduler = Executors.newScheduledThreadPool(1);
            harScheduler.scheduleAtFixedRate(() -> {
                harCaptureUtil.saveRollingHAR("TestExample", "{ \"log\": \"Live HAR Data Chunk\" }");
            }, 3, 3, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        stopHARCollection();
    }

    @Override
    public void onFinish(ITestContext context) {
        stopHARCollection();
    }

    private void stopHARCollection() {
        if (harScheduler != null) {
            harScheduler.shutdown();
        }
        if (harCaptureUtil != null) {
            harCaptureUtil.stopHARCapture();
        }
    }
}
######################################

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
 * HAR Capture Utility that saves rolling HAR files asynchronously while tests run.
 */
public class HARCaptureUtil {
    private final DevTools devTools;
    private final RemoteWebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep last 4 HAR files
    private final long maxHarSizeBytes = 2 * 1024 * 1024; // 2MB per file

    private String currentHarFile;
    private StringBuilder harBuffer = new StringBuilder();

    public HARCaptureUtil(RemoteWebDriver driver) {
        this.driver = driver;
        this.devTools = ((ChromeDriver) driver).getDevTools();
        this.devTools.createSession();
        this.currentHarFile = generateHarFileName();
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
     * Saves HAR data at intervals in rolling files.
     */
    public void saveRollingHAR(String testName, String harChunk) {
        try {
            harBuffer.append(harChunk).append("\n");

            if (harBuffer.length() >= maxHarSizeBytes) {
                flushHARFile();
            }
        } catch (Exception e) {
            System.err.println("Error saving rolling HAR: " + e.getMessage());
        }
    }

    /**
     * Stops HAR capture and ensures all data is saved.
     */
    public void stopHARCapture() {
        flushHARFile();
        System.out.println("Finalizing HAR capture...");
    }

    /**
     * Writes the buffered HAR data to a file and starts a new one.
     */
    private void flushHARFile() {
        if (harBuffer.length() == 0) return;

        try {
            saveHARToFile(harBuffer.toString(), currentHarFile);
            harFileNames.add(currentHarFile);
            if (harFileNames.size() > maxHarFiles) {
                deleteFile(harFileNames.poll());
            }
        } catch (IOException e) {
            System.err.println("Error writing HAR file: " + e.getMessage());
        }

        harBuffer = new StringBuilder();
        currentHarFile = generateHarFileName();
    }

    /**
     * Generates a new HAR file name.
     */
    private String generateHarFileName() {
        return harDirectory + "HAR_" + System.currentTimeMillis() + ".har";
    }

    /**
     * Saves HAR data to a file.
     */
    private void saveHARToFile(String harContent, String filePath) throws IOException {
        File directory = new File(harDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File harFile = new File(filePath);
        Files.write(harContent.getBytes(StandardCharsets.UTF_8), harFile);
        System.out.println("HAR file saved: " + harFile.getAbsolutePath());
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

####################################

### COMPRESSION

package utils;

import com.google.common.io.Files;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v119.network.Network;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * HAR Capture Utility that saves rolling HAR files, compresses older ones, and retains only the latest (N-1) files.
 */
public class HARCaptureUtil {
    private final DevTools devTools;
    private final RemoteWebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep last 4 uncompressed HAR files
    private final long maxHarSizeBytes = 2 * 1024 * 1024; // 2MB per file

    private String currentHarFile;
    private StringBuilder harBuffer = new StringBuilder();

    public HARCaptureUtil(RemoteWebDriver driver) {
        this.driver = driver;
        this.devTools = ((ChromeDriver) driver).getDevTools();
        this.devTools.createSession();
        this.currentHarFile = generateHarFileName();
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
     * Saves HAR data incrementally, compressing old HAR files automatically.
     */
    public void saveRollingHAR(String testName, String harChunk) {
        try {
            harBuffer.append(harChunk).append("\n");

            if (harBuffer.length() >= maxHarSizeBytes) {
                flushHARFile();
            }
        } catch (Exception e) {
            System.err.println("Error saving rolling HAR: " + e.getMessage());
        }
    }

    /**
     * Stops HAR capture and ensures final HAR data is saved.
     */
    public void stopHARCapture() {
        flushHARFile();
        System.out.println("Finalizing HAR capture...");
        compressOlderHARFiles();
    }

    /**
     * Writes the buffered HAR data to a file and starts a new one.
     */
    private void flushHARFile() {
        if (harBuffer.length() == 0) return;

        try {
            saveHARToFile(harBuffer.toString(), currentHarFile);
            harFileNames.add(currentHarFile);
            if (harFileNames.size() > maxHarFiles) {
                String oldHarFile = harFileNames.poll();
                if (oldHarFile != null) {
                    compressAndDeleteHar(oldHarFile);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing HAR file: " + e.getMessage());
        }

        harBuffer = new StringBuilder();
        currentHarFile = generateHarFileName();
    }

    /**
     * Generates a new HAR file name.
     */
    private String generateHarFileName() {
        return harDirectory + "HAR_" + System.currentTimeMillis() + ".har";
    }

    /**
     * Saves HAR data to a file.
     */
    private void saveHARToFile(String harContent, String filePath) throws IOException {
        File directory = new File(harDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File harFile = new File(filePath);
        Files.write(harContent.getBytes(StandardCharsets.UTF_8), harFile);
        System.out.println("HAR file saved: " + harFile.getAbsolutePath());
    }

    /**
     * Compresses old HAR files and deletes the original.
     */
    private void compressAndDeleteHar(String filePath) {
        String compressedFilePath = filePath + ".gz";

        try (FileInputStream fis = new FileInputStream(filePath);
             FileOutputStream fos = new FileOutputStream(compressedFilePath);
             GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                gzipOS.write(buffer, 0, len);
            }
            
            System.out.println("Compressed HAR file: " + compressedFilePath);

            // Delete original HAR file after compression
            File harFile = new File(filePath);
            if (harFile.exists() && harFile.delete()) {
                System.out.println("Deleted original HAR file: " + filePath);
            }

        } catch (IOException e) {
            System.err.println("Error compressing HAR file: " + e.getMessage());
        }
    }

    /**
     * Compresses all older HAR files except the latest (N-1).
     */
    private void compressOlderHARFiles() {
        while (harFileNames.size() > maxHarFiles) {
            String oldHarFile = harFileNames.poll();
            if (oldHarFile != null) {
                compressAndDeleteHar(oldHarFile);
            }
        }
    }
}

#######################


package listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.HARCaptureUtil;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TestNG Listener that captures HAR files asynchronously and compresses old files.
 */
public class TestListener implements ITestListener {
    private HARCaptureUtil harCaptureUtil;
    private ScheduledExecutorService harScheduler;

    @Override
    public void onStart(ITestContext context) {
        RemoteWebDriver driver = (RemoteWebDriver) context.getAttribute("driver");

        if (driver != null) {
            harCaptureUtil = new HARCaptureUtil(driver);
            harCaptureUtil.startHARCapture();

            // Run HAR saving in a background thread every 3 seconds
            harScheduler = Executors.newScheduledThreadPool(1);
            harScheduler.scheduleAtFixedRate(() -> {
                harCaptureUtil.saveRollingHAR("TestExample", "{ \"log\": \"Live HAR Data Chunk\" }");
            }, 3, 3, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        stopHARCollection();
    }

    @Override
    public void onFinish(ITestContext context) {
        stopHARCollection();
    }

    private void stopHARCollection() {
        if (harScheduler != null) {
            harScheduler.shutdown();
        }
        if (harCaptureUtil != null) {
            harCaptureUtil.stopHARCapture();
        }
    }
}








