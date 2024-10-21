import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A thread that periodically sends a WebDriver no-op command (via executeScript)
 * to prevent the session from timing out due to idleness during long-running, non-Web UI operations.
 * 
 * This class will keep the session alive by executing a harmless JavaScript command in the browser, 
 * preventing the WebDriver session from timing out when no direct WebDriver interaction occurs for 
 * an extended period of time.
 */
public class KeepAliveThread extends Thread {
    private final WebDriver driver;
    private volatile boolean stopRequested = false;

    /**
     * Constructor to initialize the KeepAliveThread with the WebDriver instance.
     * 
     * @param driver The WebDriver instance used to send keep-alive commands to the remote browser.
     */
    public KeepAliveThread(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Requests to stop the keep-alive thread after the long-running operation completes.
     */
    public void requestStop() {
        stopRequested = true;
    }

    /**
     * The run method that periodically sends a no-op JavaScript command to keep the session alive.
     * 
     * This method will execute every 30 seconds (configurable) while the long-running operation is 
     * performed, ensuring that the WebDriver session remains active. The method will stop executing 
     * once `requestStop` is called.
     */
    @Override
    public void run() {
        try {
            while (!stopRequested) {
                // Use executeScript to send a harmless JavaScript command to the browser
                ((JavascriptExecutor) driver).executeScript("return true;");
                System.out.println("Keeping session alive by executing a no-op JavaScript command...");
                
                // Sleep for 30 seconds (adjust this interval as needed to prevent session timeout)
                Thread.sleep(30000); // Sleep for 30 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }
}

/**
 * Test case demonstrating how to use KeepAliveThread to keep the WebDriver session alive
 * during long-running non-Web UI operations.
 * 
 * This class contains:
 * 1. A setup method to initialize the WebDriver and start the keep-alive thread.
 * 2. A test method that simulates a long-running non-Web UI operation (e.g., a background task) 
 *    while the keep-alive thread periodically keeps the WebDriver session active.
 * 3. A teardown method to stop the keep-alive thread and quit the WebDriver after the test.
 */
public class LongRunningTest {
    WebDriver driver;
    KeepAliveThread keepAliveThread;

    /**
     * Setup method to initialize WebDriver and start the KeepAliveThread.
     * 
     * This method sets up the ChromeDriver (or any WebDriver instance, such as a remote WebDriver) 
     * and starts the KeepAliveThread to prevent the session from timing out during long-running operations.
     */
    @BeforeClass
    public void setUp() {
        // Initialize WebDriver (can be set up to use remote WebDriver if needed)
        driver = new ChromeDriver();
        
        // Start the KeepAliveThread to keep the session alive during long operations
        keepAliveThread = new KeepAliveThread(driver);
        keepAliveThread.start();
    }

    /**
     * Test method simulating a long-running non-Web UI operation.
     * 
     * The test will start the keep-alive thread and perform a simulated long-running operation 
     * (e.g., a task that takes over an hour). After the long operation, it proceeds with WebDriver actions.
     * The keep-alive thread ensures that the WebDriver session does not time out during the operation.
     */
    @Test
    public void longRunningOperationTest() {
        try {
            // Perform your long-running non-WebUI operation here
            performLongRunningOperation();

            // After the long operation, you can proceed with any WebDriver actions
            driver.get("https://example.com");
            Assert.assertTrue(driver.getTitle().contains("Example"), "Page title did not contain 'Example'");
        } finally {
            // Ensure the keep-alive thread is stopped after the test
            keepAliveThread.requestStop();
        }
    }

    /**
     * Simulates a long-running non-Web UI operation (e.g., a background task that takes more than an hour).
     * 
     * This method simulates a task that takes 1 hour using `Thread.sleep()`. In real scenarios, replace this 
     * with your actual non-Web UI operation that requires the WebDriver session to stay alive.
     */
    private void performLongRunningOperation() {
        System.out.println("Starting long-running non-WebUI operation...");
        try {
            // Simulating a 1-hour long operation using Thread.sleep()
            Thread.sleep(3600000); // Sleep for 1 hour (3600000 milliseconds)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
        System.out.println("Long-running operation completed.");
    }

    /**
     * Teardown method to stop the keep-alive thread and quit the WebDriver.
     * 
     * This method ensures that after the test execution, the KeepAliveThread is stopped, and the WebDriver session 
     * is properly terminated.
     */
    @AfterClass
    public void tearDown() {
        // Quit the WebDriver session at the end of the test
        if (driver != null) {
            driver.quit();
        }
    }
}
