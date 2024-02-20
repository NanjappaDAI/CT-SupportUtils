package tests;

import helpers.Helpers;
import helpers.PropertiesReader;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static org.testng.Assert.assertTrue;

public class SelfHealingTest {

    protected ThreadLocal<AppiumDriver> driver = new ThreadLocal<>();
    protected ThreadLocal<WebDriverWait> wait = new ThreadLocal<>();
    protected ThreadLocal<DesiredCapabilities> desiredCapabilities = new ThreadLocal<>();

    protected Helpers helper;

    public AppiumDriver getDriver() {
        return driver.get();
    }

    public WebDriverWait getWait() {
        return wait.get();
    }

    @BeforeMethod
    @Parameters({"application.name"})
    public void setUp(@Optional ITestContext context, @Optional Method method, String applicationName) throws MalformedURLException {

        helper = new Helpers();
        String deviceQuery = context.getSuite().getParameter("device.query");

        if (applicationName.equalsIgnoreCase("unmodified")) {
            helper.upload_application_api(new PropertiesReader().getProperty("unmodified.build"), applicationName);
        } else if (applicationName.equalsIgnoreCase("modified")) {
            helper.upload_application_api(new PropertiesReader().getProperty("modified.build"), applicationName);
        }

        DesiredCapabilities caps = new DesiredCapabilities();

        caps.setCapability("testName", method.getName());
        caps.setCapability("accessKey", new PropertiesReader().getProperty("ct.accessKey"));
        caps.setCapability("appiumVersion", "2.1.3");
        caps.setCapability("automationName", "UIAutomator2");
//                caps.setCapability("deviceQuery", deviceQuery);
        caps.setCapability("udid", "21081FDF6004UB");
//        caps.setCapability("deviceQuery", "@os='android'");
        caps.setCapability("app", "cloud:uniqueName=" + applicationName);
        caps.setCapability("appPackage", "com.experitest.ExperiBank");
        caps.setCapability("appActivity", ".LoginActivity");
        caps.setCapability("fullReset", true);
        caps.setCapability("autoGrantPermissions", true);
        caps.setCapability("selfHealing", true);

        desiredCapabilities.set(caps);
        driver.set(new AndroidDriver(new URL(new PropertiesReader().getProperty("ct.cloudUrl") + "/wd/hub"), caps));
        wait.set(new WebDriverWait(getDriver(), Duration.ofSeconds(5)));
        System.out.println("setUp() - Initializating the Session");
    }

    @Test
    public void login_scenario_test() throws InterruptedException {
        System.out.println("login_scenario_test() - Starting the Test");
        getWait().until(ExpectedConditions.elementToBeClickable(By.id("com.experitest.ExperiBank:id/usernameTextField")));
        getDriver().findElement(By.id("com.experitest.ExperiBank:id/usernameTextField")).sendKeys("company");
        getDriver().findElement(By.id("com.experitest.ExperiBank:id/passwordTextField")).sendKeys("company");
        getDriver().findElement(By.id("com.experitest.ExperiBank:id/loginButton")).click();

        Thread.sleep(3000);

        Boolean isLogoutButtonPresent = getDriver().findElement(By.id("com.experitest.ExperiBank:id/makePaymentButton")).isDisplayed();
        assertTrue(isLogoutButtonPresent);
        System.out.println("login_scenario_test() - Ending the Test");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        System.out.println("tearDown() - Closing the Session");
        try {
            if (result.isSuccess()) {
                String capsval = String.valueOf(getDriver().getCapabilities().getCapabilityNames());
                System.out.println(capsval);
                getDriver().executeScript("seetest:client.setReportStatus", "Passed", "Test Passed");
            } else {

                getDriver().executeScript("seetest:client.setReportStatus", "Failed", "Test Failed");
            }
        } catch (Exception e) {}

        if (driver != null) {
            getDriver().quit();
            driver.remove();
            wait.remove();
        }
    }

}
