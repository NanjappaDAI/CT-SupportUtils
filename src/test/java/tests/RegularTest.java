package tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.testng.annotations.*;
import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;

public class RegularTest {

    protected AndroidDriver driver = null;
    DesiredCapabilities dc = new DesiredCapabilities();
//    private String accessKey = "eyJhbGciOiJIUzI1NiJ9.eyJ4cC51IjoxMzc1NDIsInhwLnAiOjEzNzU0NCwieHAubSI6MTcwMTM0MTAyMDUxOCwiZXhwIjoyMDE2NzAxMDIwLCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.oYcBfYSD09bs9kaWJUDfWewcAMJPZEPkg1_NqyoKH5w";
    private String accessKey = "eyJhbGciOiJIUzI1NiJ9.eyJ4cC51Ijo0NywieHAucCI6MiwieHAubSI6MTY2MjA5ODQxMzcyNSwiZXhwIjoyMDIzNzA0MzM1LCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.aqzoIRGbWM-5ME38GliYerR7qlPthIP18yVyEbhihLY";
    private String uid = System.getenv("deviceID");
    private String status = "failed";

    @BeforeTest
    public void setUp() throws MalformedURLException {
        dc.setCapability("testName", "Cleanup Webhook");
        dc.setCapability("accessKey", accessKey);
        dc.setCapability("releaseDevice", false);
        dc.setCapability("deviceQuery", "@serialnumber='" + uid + "'");
//        dc.setCapability("udid", "RFCR301SQCY");
        dc.setCapability("appiumVersion", "2.1.3");
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
        status = "passed";

    }

    @AfterTest
    public void tearDown() {
        FinishCleanupState();
        System.out.println("Report URL: " + driver.getCapabilities().getCapability("reportUrl"));
        driver.quit();
    }

    private void FinishCleanupState() {
        HttpPost post = new HttpPost("https://internal.experitest.com/api/v1/cleanup-finish?deviceId=" + uid + "&status=" + status);
        post.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + accessKey);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {
        } catch (Exception ignore){ }
    }
}