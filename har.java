package utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Optional;

/**
 * HAR Capture Utility that dynamically detects the highest available CDP version,
 * captures real network data, and manages file size by splitting large HAR files.
 * This implementation works for both Local and Remote WebDriver without fixed imports.
 */
public class HARCaptureUtil {
    private DevTools devTools;
    private final WebDriver driver;
    private final String harDirectory = "target/har-logs/";
    private final LinkedList<String> harFileNames = new LinkedList<>();
    private final int maxHarFiles = 5;  // Keep last 4 uncompressed HAR files
    private final long maxHarSizeBytes = 2 * 1024 * 1024; // 2MB per file
    private Object networkInstance; // Holds the dynamically loaded Network class instance
    private Class<?> responseReceivedClass; // Holds the dynamically loaded ResponseReceived event class

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
            // Local ChromeDriver: Directly get DevTools
            this.devTools = ((ChromeDriver) driver).getDevTools();
            this.devTools.createSession();
            System.out.println("Initialized DevTools for Local ChromeDriver.");
        } else if (driver instanceof RemoteWebDriver) {
            RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;

            // Attempt to get WebSocket Debugger URL from ChromeOptions
            Object cdpCapability = remoteDriver.getCapabilities().getCapability("goog:chromeOptions");
            if (cdpCapability instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> chromeOptions = (Map<String, Object>) cdpCapability;

                if (chromeOptions.containsKey("debuggerAddress")) {
                    String debuggerAddress = chromeOptions.get("debuggerAddress").toString();
                    System.out.println("Using WebSocket Debugger URL: " + debuggerAddress);

                    // Use WebSocket URL to attach DevTools manually
                    this.devTools = remoteDriver.getDevTools();
                    this.devTools.createSession();
                    System.out.println("Successfully attached DevTools for RemoteWebDriver.");
                } else {
                    System.err.println("No debugger address found in ChromeOptions.");
                }
            } else {
                System.err.println("DevTools is not available for RemoteWebDriver (no 'goog:chromeOptions' capability).");
            }
        }
    } catch (Exception e) {
        System.err.println("Error initializing DevTools: " + e.getMessage());
    }
}

    /**
     * Dynamically finds and loads the highest available Network module using reflection.
     */
    private Object getHighestNetworkInstance() {
        String basePackage = "org.openqa.selenium.devtools";

        for (int version = 199; version >= 110; version--) {
            String className = basePackage + ".v" + version + ".network.Network";
            String eventClassName = basePackage + ".v" + version + ".network.model.ResponseReceived";
            try {
                Class<?> networkClass = Class.forName(className);
                responseReceivedClass = Class.forName(eventClassName);
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

            Method enableMethod = networkInstance.getClass().getMethod("enable", Optional.class, Optional.class, Optional.class);
            enableMethod.invoke(networkInstance, Optional.empty(), Optional.empty(), Optional.empty());

            // Create a dynamic proxy listener for responseReceived
            Object proxyListener = Proxy.newProxyInstance(
                    responseReceivedClass.getClassLoader(),
                    new Class<?>[]{responseReceivedClass},
                    (proxy, method, args) -> {
                        if ("accept".equals(method.getName()) && args.length == 1) {
                            captureHARChunk(args[0]); // Call captureHARChunk dynamically
                        }
                        return null;
                    });

            // Add listener dynamically for responseReceived event
            Method addListenerMethod = devTools.getClass().getMethod("addListener", Class.class, Object.class);
            addListenerMethod.invoke(devTools, responseReceivedClass, proxyListener);

            System.out.println("HAR capture started with dynamic CDP version...");
        } catch (Exception e) {
            System.err.println("Error starting HAR capture: " + e.getMessage());
        }
    }

    /**
     * Captures HAR data in small chunks dynamically.
     */
    private void captureHARChunk(Object response) {
        try {
            Method getUrlMethod = response.getClass().getMethod("getUrl");
            Method getStatusMethod = response.getClass().getMethod("getStatus");

            String requestUrl = (String) getUrlMethod.invoke(response);
            int status = (int) getStatusMethod.invoke(response);

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

            if ((currentFileSize + chunkSize) >= maxHarSizeBytes) {
                flushHARFile();
            }

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
                String oldFile = harFileNames.poll();
                if (oldFile != null) {
                    new File(oldFile).delete();
                }
            }

            initializeHarFile();
        } catch (IOException e) {
            System.err.println("Error finalizing HAR file: " + e.getMessage());
        }
    }

/**
 * Initializes a new HAR file for writing.
 */
private void initializeHarFile() {
    try {
        // Ensure the target directory exists
        File directory = new File(harDirectory);
        if (!directory.exists()) {
            boolean dirCreated = directory.mkdirs();
            if (dirCreated) {
                System.out.println("Created directory: " + harDirectory);
            } else {
                System.err.println("Failed to create directory: " + harDirectory);
            }
        }

        // Generate the HAR file name
        currentHarFile = harDirectory + File.separator + "HAR_" + System.currentTimeMillis() + ".har";
        
        // Create a new file if it doesn't exist
        File harFile = new File(currentHarFile);
        if (!harFile.exists()) {
            boolean fileCreated = harFile.createNewFile();
            if (fileCreated) {
                System.out.println("Created HAR file: " + currentHarFile);
            } else {
                System.err.println("Failed to create HAR file: " + currentHarFile);
            }
        }

        // Open file writer in append mode
        fileWriter = new FileWriter(harFile, true);
        currentFileSize = harFile.length();

    } catch (IOException e) {
        System.err.println("Error initializing HAR file: " + e.getMessage());
    }
}
}




###################################################################


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
            System.out.println("Initialized DevTools for Local ChromeDriver.");
        } else if (driver instanceof RemoteWebDriver) {
            RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;

            // Attempt to get WebSocket Debugger URL from ChromeOptions
            Object cdpEndpoint = remoteDriver.getCapabilities().getCapability("goog:chromeOptions");
            if (cdpEndpoint != null && cdpEndpoint instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> chromeOptions = (Map<String, Object>) cdpEndpoint;

                if (chromeOptions.containsKey("debuggerAddress")) {
                    String debuggerAddress = chromeOptions.get("debuggerAddress").toString();
                    System.out.println("Using WebSocket Debugger URL: " + debuggerAddress);

                    // Attach DevTools manually via WebSocket URL
                    this.devTools = DevTools.createSession(debuggerAddress);
                    this.devTools.createSession();
                    System.out.println("Successfully attached DevTools for RemoteWebDriver.");
                } else {
                    System.err.println("No debugger address found in ChromeOptions.");
                }
            } else {
                System.err.println("DevTools is not available for RemoteWebDriver (no 'goog:chromeOptions' capability).");
            }
        }
    } catch (Exception e) {
        System.err.println("Error initializing DevTools: " + e.getMessage());
    }
}

