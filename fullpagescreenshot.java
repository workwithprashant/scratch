import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
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
     * This method accurately scrolls the page in segments, captures multiple screenshots, and stitches them.
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
            int totalHeight = ((Long) js.executeScript("return document.documentElement.scrollHeight")).intValue();
            int viewportHeight = ((Long) js.executeScript("return window.innerHeight")).intValue();
            int totalWidth = ((Long) js.executeScript("return document.documentElement.scrollWidth")).intValue();

            List<BufferedImage> images = new ArrayList<>();
            int currentScroll = 0;
            int scrollCount = 0;

            while (currentScroll < totalHeight) {
                // Scroll to the next segment accurately
                js.executeScript("window.scrollTo(0, arguments[0]);", currentScroll);
                Thread.sleep(500); // Allow some time for rendering

                // Capture the viewport screenshot
                byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                BufferedImage screenshot = ImageIO.read(new java.io.ByteArrayInputStream(screenshotBytes));

                // Adjust last screenshot height to avoid overlap
                if (currentScroll + viewportHeight > totalHeight) {
                    int extraHeight = (currentScroll + viewportHeight) - totalHeight;
                    screenshot = screenshot.getSubimage(0, extraHeight, screenshot.getWidth(), screenshot.getHeight() - extraHeight);
                }

                images.add(screenshot);
                currentScroll += viewportHeight;
                scrollCount++;
            }

            LOGGER.info("Captured " + scrollCount + " segments. Stitching images...");

            // Create final stitched image
            int finalHeight = images.stream().mapToInt(BufferedImage::getHeight).sum();
            BufferedImage finalImage = new BufferedImage(totalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB);
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



##############################################################################

WebDriver driver = new RemoteWebDriver(new URL("http://your.selenium.grid/wd/hub"), options);

String base64Screenshot = ScreenshotUtil.captureFullPageScreenshotBase64(driver);
if (!base64Screenshot.isEmpty()) {
    System.out.println("Screenshot captured successfully!");
} else {
    System.out.println("Screenshot capture failed, but test continues.");
}
