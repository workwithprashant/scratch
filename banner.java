import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * BaseTest class to handle WebDriver setup and StepBanner utility.
 * Every test class should extend this class to inherit common functionality.
 */
public class BaseTest {
    protected WebDriver driver;  // WebDriver instance for browser automation
    protected StepBannerUtil banner;  // Step banner utility for displaying execution steps

    /**
     * This method runs before any test in the child classes.
     * It sets up the WebDriver and injects the step banner into the browser.
     */
    @BeforeClass
    public void setUp() {
        System.setProperty("webdriver.chrome.driver", "path/to/chromedriver"); // Set the ChromeDriver path
        driver = new ChromeDriver();  // Launch Chrome browser
        driver.manage().window().maximize(); // Maximize browser window
        driver.get("https://example.com"); // Open test URL

        // Initialize the step banner utility
        banner = new StepBannerUtil(driver);
    }

    /**
     * This method runs after all tests in the child classes.
     * It ensures that the step banner is removed and the browser is closed.
     */
    @AfterClass
    public void tearDown() {
        if (banner != null) {
            banner.removeBanner(); // Ensure banner is removed after test execution
        }
        if (driver != null) {
            driver.quit(); // Close the browser
        }
    }
}


####################################################

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Utility class to inject a floating step banner into the browser.
 * The banner displays test execution steps dynamically.
 */
public class StepBannerUtil {
    private JavascriptExecutor js; // JavaScript Executor to manipulate the browser UI

    /**
     * Constructor: Injects the banner when the utility is initialized.
     * @param driver The WebDriver instance controlling the browser.
     */
    public StepBannerUtil(WebDriver driver) {
        this.js = (JavascriptExecutor) driver;
        injectBanner();
    }

    /**
     * Injects a floating banner into the browser window using JavaScript.
     * The banner appears at the bottom center and remains visible throughout the test execution.
     */
    private void injectBanner() {
        String script = "var stepBanner = document.createElement('div');"
                + "stepBanner.id = 'selenium-step-banner';"
                + "stepBanner.style.position = 'fixed';"
                + "stepBanner.style.bottom = '10px';"
                + "stepBanner.style.left = '50%';"
                + "stepBanner.style.transform = 'translateX(-50%)';"
                + "stepBanner.style.backgroundColor = 'rgba(0, 0, 0, 0.8)';"
                + "stepBanner.style.color = 'white';"
                + "stepBanner.style.padding = '10px 20px';"
                + "stepBanner.style.fontSize = '16px';"
                + "stepBanner.style.borderRadius = '8px';"
                + "stepBanner.style.zIndex = '9999';"
                + "stepBanner.style.boxShadow = '0px 0px 10px rgba(0,0,0,0.5)';"
                + "stepBanner.style.transition = 'opacity 0.5s ease-in-out';"
                + "stepBanner.style.opacity = '0.9';"
                + "document.body.appendChild(stepBanner);"
                + "stepBanner.innerText = 'Executing Test...';";
        js.executeScript(script);
    }

    /**
     * Updates the banner text dynamically to reflect the current test step.
     * @param stepDescription The step description to display.
     */
    public void updateStep(String stepDescription) {
        String script = "var banner = document.getElementById('selenium-step-banner');"
                + "if (banner) { banner.innerText = arguments[0]; }";
        js.executeScript(script, stepDescription);
    }

    /**
     * Removes the banner from the browser UI after test execution.
     */
    public void removeBanner() {
        String script = "var banner = document.getElementById('selenium-step-banner');"
                + "if (banner) { banner.remove(); }";
        js.executeScript(script);
    }
}



####################################################
