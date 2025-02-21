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



