import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * ### How It Works:
 *
 * 1. Before the test starts, the KeepAliveThread is initialized with the WebDriver instance.
 * 2. During the long-running operation, the KeepAliveThread periodically sends a no-op command (via executeScript)
 *    to keep the browser session active.
 * 3. Once the operation completes, the test can continue with WebDriver actions without losing the session.
 * 4. The keep-alive thread is stopped after the long operation and the WebDriver session is properly terminated
 *    in the teardown phase.
 *
 * ### Example Code:
 * <pre>
 * {@code
 * public class LongRunningTest {
 *     WebDriver driver;
 *     KeepAliveThread keepAliveThread;
 * 
 *     @BeforeClass
 *     public void setUp() {
 *         driver = new ChromeDriver();
 *         keepAliveThread = new KeepAliveThread(driver);
 *         keepAliveThread.start();
 *     }
 * 
 *     @Test
 *     public void longRunningOperationTest() {
 *         try {
 *             performLongRunningOperation();
 *             driver.get("https://example.com");
 *             Assert.assertTrue(driver.getTitle().contains("Example"));
 *         } finally {
 *             keepAliveThread.requestStop();
 *         }
 *     }
 * 
 *     private void performLongRunningOperation() {
 *         try {
 *             Thread.sleep(3600000); // Simulates a long-running 1-hour operation
 *         } catch (InterruptedException e) {
 *             Thread.currentThread().interrupt();
 *         }
 *     }
 * 
 *     @AfterClass
 *     public void tearDown() {
 *         if (driver != null) {
 *             driver.quit();
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * ### Notes:
 * - Always ensure that you stop the KeepAliveThread after your long-running task to prevent it from continuing indefinitely.
 * - Adjust the sleep interval in the KeepAliveThread (currently 30 seconds) according to your session timeout requirements.
 */


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
