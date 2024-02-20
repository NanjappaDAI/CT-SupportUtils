package tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.testng.annotations.*;
import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;

public class RegularTest {

    protected AndroidDriver driver = null;
    DesiredCapabilities dc = new DesiredCapabilities();
    private String accessKey = "eyJhbGciOiJIUzI1NiJ9.eyJ4cC51IjoxMzc1NDIsInhwLnAiOjEzNzU0NCwieHAubSI6MTcwMTM0MTAyMDUxOCwiZXhwIjoyMDE2NzAxMDIwLCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.oYcBfYSD09bs9kaWJUDfWewcAMJPZEPkg1_NqyoKH5w";

    @BeforeTest
    public void setUp() throws MalformedURLException {
        dc.setCapability("testName", "Quick Start Android Native Demo");
        dc.setCapability("accessKey", accessKey);
//        dc.setCapability("deviceQuery", "@os='android' and @category='PHONE'");
        dc.setCapability("udid", "RFCR301SQCY");
//        dc.setCapability(MobileCapabilityType.APP, "cloud:com.experitest.ExperiBank/.LoginActivity");
        dc.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, "com.experitest.ExperiBank");
        dc.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, ".LoginActivity");
        driver = new AndroidDriver(new URL("https://internal.experitest.com/wd/hub"), dc);
    }

    @Test
    public void quickStartAndroidNativeDemo() {
        driver.rotate(ScreenOrientation.PORTRAIT);
//        driver.findElement(By.id("com.experitest.ExperiBank:id/usernameTextField")).sendKeys("company");
//        driver.findElement(By.id("com.experitest.ExperiBank:id/passwordTextField")).sendKeys("company");
//        driver.findElement(By.id("com.experitest.ExperiBank:id/loginButton")).click();
//        driver.findElement(By.id("com.experitest.ExperiBank:id/makePaymentButton")).click();
//        driver.findElement(By.id("com.experitest.ExperiBank:id/phoneTextField")).sendKeys("0501234567");
//        driver.findElement(By.id("com.experitest.ExperiBank:id/nameTextField")).sendKeys("John Snow");
//        driver.findElement(By.id("com.experitest.ExperiBank:id/amountTextField")).sendKeys("50");
//        driver.findElement(By.id("com.experitest.ExperiBank:id/countryTextField")).sendKeys("'Switzerland'");
//        driver.findElement(By.id("com.experitest.ExperiBank:id/sendPaymentButton")).click();
//        driver.findElement(By.id("android:id/button1")).click();
    }

    @AfterTest
    public void tearDown() {
        System.out.println("Report URL: " + driver.getCapabilities().getCapability("reportUrl"));
        driver.quit();
    }
}