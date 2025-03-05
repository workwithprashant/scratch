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



################################

package utils;

import com.google.common.io.Files;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v119.network.Network;
import org.openqa.selenium.devtools.v119.network.model.ResponseReceived;
import org.openqa.selenium.devtools.v119.network.model.RequestId;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * Utility to capture HAR files, split them by size, and compress old HAR files.
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
     * Starts real HAR capture using Chrome DevTools Protocol (CDP).
     */
    public void startHARCapture() {
        try {
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            // Capture Network Responses
            devTools.addListener(Network.responseReceived(), (ResponseReceived response) -> {
                captureHARChunk(response);
            });

            System.out.println("HAR capture started...");
        } catch (Exception e) {
            System.err.println("Error starting HAR capture: " + e.getMessage());
        }
    }

    /**
     * Captures HAR data in small chunks.
     */
    private void captureHARChunk(ResponseReceived response) {
        try {
            RequestId requestId = response.getRequestId();
            String requestUrl = response.getResponse().getUrl();
            String status = response.getResponse().getStatus().toString();
            
            // Get Response Body (Optional: Uncomment for full response capture)
            String responseBody = devTools.send(Network.getResponseBody(requestId)).getBody();
            
            // Construct HAR-like JSON entry
            String harChunk = "{ \"url\": \"" + requestUrl + "\", \"status\": " + status + ", \"response\": \"" + responseBody + "\" }";
            
            // Save HAR chunk
            saveRollingHAR(harChunk);

        } catch (Exception e) {
            System.err.println("Error capturing HAR chunk: " + e.getMessage());
        }
    }

    /**
     * Saves HAR data in rolling files.
     */
    public void saveRollingHAR(String harChunk) {
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
     * Stops HAR capture and saves remaining data.
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



###########

import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.OutputType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FullPageScreenshotUtil {
    private static final Logger LOGGER = Logger.getLogger(FullPageScreenshotUtil.class.getName());

    /**
     * Captures a full-page screenshot as a Base64 string on a remote Selenium Grid.
     * This method scrolls through the page in segments, takes multiple screenshots, and stitches them.
     *
     * @param driver The WebDriver instance (RemoteWebDriver recommended)
     * @return Base64 string of the full-page screenshot or empty string on failure.
     */
    public static String captureFullPageScreenshotBase64(WebDriver driver) {
        if (!(driver instanceof RemoteWebDriver)) {
            LOGGER.warning("This method is designed for RemoteWebDriver instances.");
            return "";
        }

        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Get page dimensions
            int totalHeight = ((Long) js.executeScript("return document.body.scrollHeight")).intValue();
            int viewportHeight = ((Long) js.executeScript("return window.innerHeight")).intValue();
            int totalWidth = ((Long) js.executeScript("return document.body.scrollWidth")).intValue();

            List<BufferedImage> images = new ArrayList<>();
            int currentScroll = 0;

            while (currentScroll < totalHeight) {
                js.executeScript("window.scrollTo(0, arguments[0]);", currentScroll);
                Thread.sleep(500); // Allow some time for rendering
                
                // Capture the current viewport screenshot
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                BufferedImage screenshot = ImageIO.read(new java.io.ByteArrayInputStream(screenshotBytes));
                images.add(screenshot);

                currentScroll += viewportHeight;
            }

            // Create final stitched image
            BufferedImage finalImage = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = finalImage.createGraphics();

            int yOffset = 0;
            for (BufferedImage img : images) {
                g2d.drawImage(img, 0, yOffset, null);
                yOffset += img.getHeight();
            }
            g2d.dispose();

            // Convert stitched image to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error capturing full-page screenshot.", e);
            return ""; // Return empty string to prevent test failure
        }
    }
}







#########################


package utils;

import com.google.common.io.Files;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * HAR Capture Utility that dynamically detects the highest available CDP version
 * and supports both local ChromeDriver and RemoteWebDriver (Selenium Grid).
 */
public class HARCaptureUtil {
    private DevTools devTools;
    private final WebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep last 4 uncompressed HAR files
    private final long maxHarSizeBytes = 2 * 1024 * 1024; // 2MB per file
    private Object networkInstance; // Holds the dynamically loaded Network class instance

    private String currentHarFile;
    private StringBuilder harBuffer = new StringBuilder();

    public HARCaptureUtil(WebDriver driver) {
        this.driver = driver;
        initializeDevTools();
        this.currentHarFile = generateHarFileName();
    }

    /**
     * Initializes DevTools session for Chrome.
     * Supports both local ChromeDriver and RemoteWebDriver.
     */
    private void initializeDevTools() {
        try {
            if (driver instanceof ChromeDriver) {
                // Local ChromeDriver: Directly get DevTools
                this.devTools = ((ChromeDriver) driver).getDevTools();
                this.devTools.createSession();
                System.out.println("Successfully initialized DevTools for Local ChromeDriver.");
            } else if (driver instanceof RemoteWebDriver) {
                // Remote WebDriver (Selenium Grid / Selenium Box)
                Object bridge = ((RemoteWebDriver) driver).getCapabilities().getCapability("se:cdp");
                if (bridge != null) {
                    devTools = (DevTools) bridge;
                    devTools.createSession();
                    System.out.println("Successfully initialized DevTools for RemoteWebDriver.");
                } else {
                    System.err.println("DevTools is not available for RemoteWebDriver.");
                }
            }

            // Dynamically load the highest available Network module
            networkInstance = getHighestNetworkInstance();
        } catch (Exception e) {
            System.err.println("Error initializing DevTools: " + e.getMessage());
        }
    }

    /**
     * Dynamically finds and loads the highest available Network module using reflection.
     */
    private Object getHighestNetworkInstance() {
        String basePackage = "org.openqa.selenium.devtools";

        // Start from v199 and go down to v110, stopping at the first available version
        for (int version = 199; version >= 110; version--) {
            String className = basePackage + ".v" + version + ".network.Network";
            try {
                Class<?> networkClass = Class.forName(className);
                System.out.println("Using Chrome DevTools Protocol version: " + version);
                return networkClass.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException ignored) {
                // Continue checking lower versions until we find the latest available
            }
        }
        System.err.println("No valid Chrome DevTools Network version found.");
        return null;
    }

    /**
     * Starts HAR capture using the dynamically loaded CDP Network module.
     */
    public void startHARCapture() {
        try {
            if (devTools == null || networkInstance == null) {
                System.err.println("DevTools is not initialized, HAR capture cannot start.");
                return;
            }

            // Enable Network Monitoring using reflection
            Method enableMethod = networkInstance.getClass().getMethod("enable", Optional.class, Optional.class, Optional.class);
            enableMethod.invoke(networkInstance, Optional.empty(), Optional.empty(), Optional.empty());

            System.out.println("HAR capture started with dynamic CDP version...");
        } catch (Exception e) {
            System.err.println("Error starting HAR capture: " + e.getMessage());
        }
    }

    /**
     * Saves HAR data in rolling files.
     */
    public void saveRollingHAR(String harChunk) {
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
     * Stops HAR capture and saves remaining data.
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

########################

package utils;

import com.google.common.io.Files;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v119.network.Network;
import org.openqa.selenium.devtools.v119.network.model.ResponseReceived;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

/**
 * HAR Capture Utility that dynamically detects the highest available CDP version,
 * captures real network data, and manages file size by splitting large HAR files.
 */
public class HARCaptureUtil {
    private DevTools devTools;
    private final WebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep last 4 uncompressed HAR files
    private final long maxHarSizeBytes = 2 * 1024 * 1024; // 2MB per file

    private String currentHarFile;
    private FileWriter fileWriter;
    private long currentFileSize = 0; // Track actual file size

    public HARCaptureUtil(WebDriver driver) {
        this.driver = driver;
        initializeDevTools();
        initializeHarFile();
    }

    /**
     * Initializes DevTools session for Chrome.
     * Supports both local ChromeDriver and RemoteWebDriver.
     */
    private void initializeDevTools() {
        try {
            if (driver instanceof ChromeDriver) {
                this.devTools = ((ChromeDriver) driver).getDevTools();
                this.devTools.createSession();
                System.out.println("Initialized DevTools for Local ChromeDriver.");
            } else if (driver instanceof RemoteWebDriver) {
                Object bridge = ((RemoteWebDriver) driver).getCapabilities().getCapability("se:cdp");
                if (bridge != null) {
                    devTools = (DevTools) bridge;
                    devTools.createSession();
                    System.out.println("Initialized DevTools for RemoteWebDriver.");
                } else {
                    System.err.println("DevTools is not available for RemoteWebDriver.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing DevTools: " + e.getMessage());
        }
    }

    /**
     * Initializes the HAR file for writing.
     */
    private void initializeHarFile() {
        try {
            currentHarFile = generateHarFileName();
            fileWriter = new FileWriter(currentHarFile, true); // Append mode
            currentFileSize = new File(currentHarFile).length();
        } catch (IOException e) {
            System.err.println("Error initializing HAR file: " + e.getMessage());
        }
    }

    /**
     * Starts HAR capture using Chrome DevTools Protocol.
     */
    public void startHARCapture() {
        try {
            if (devTools == null) {
                System.err.println("DevTools not initialized, cannot start HAR capture.");
                return;
            }
            devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            // Capture Network Responses
            devTools.addListener(Network.responseReceived(), this::captureHARChunk);

            System.out.println("HAR capture started...");
        } catch (Exception e) {
            System.err.println("Error starting HAR capture: " + e.getMessage());
        }
    }

    /**
     * Captures HAR data in small chunks.
     */
    private void captureHARChunk(ResponseReceived response) {
        try {
            String requestUrl = response.getResponse().getUrl();
            int status = response.getResponse().getStatus();
            String harEntry = "{ \"url\": \"" + requestUrl + "\", \"status\": " + status + " },\n";

            saveRollingHAR(harEntry);
        } catch (Exception e) {
            System.err.println("Error capturing HAR chunk: " + e.getMessage());
        }
    }

    /**
     * Saves HAR data in rolling files, managing size limits.
     */
    public void saveRollingHAR(String harChunk) {
        try {
            byte[] harBytes = harChunk.getBytes(StandardCharsets.UTF_8);
            long chunkSize = harBytes.length;

            // If adding this chunk exceeds the max file size, flush to a new HAR file
            if ((currentFileSize + chunkSize) >= maxHarSizeBytes) {
                flushHARFile();
            }

            // Write the chunk to the current HAR file
            fileWriter.write(harChunk);
            fileWriter.flush();
            currentFileSize += chunkSize;

        } catch (IOException e) {
            System.err.println("Error saving rolling HAR: " + e.getMessage());
        }
    }

    /**
     * Stops HAR capture and saves remaining data.
     */
    public void stopHARCapture() {
        flushHARFile();
        System.out.println("Finalizing HAR capture...");
        compressOlderHARFiles();
    }

    /**
     * Finalizes the current HAR file and prepares a new one.
     */
    private void flushHARFile() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
            }
            harFileNames.add(currentHarFile);
            if (harFileNames.size() > maxHarFiles) {
                compressAndDeleteHar(harFileNames.poll());
            }

            // Initialize a new HAR file
            initializeHarFile();

        } catch (IOException e) {
            System.err.println("Error finalizing HAR file: " + e.getMessage());
        }
    }

    /**
     * Generates a new HAR file name.
     */
    private String generateHarFileName() {
        return harDirectory + "HAR_" + System.currentTimeMillis() + ".har";
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
            compressAndDeleteHar(harFileNames.poll());
        }
    }
}
