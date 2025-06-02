package tests;

import com.experitest.appium.SeeTestClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class CheckLisbonDevicesTest {

    protected AppiumDriver driver = null;
    DesiredCapabilities dc = new DesiredCapabilities();
    protected SeeTestClient seetest = null;

    @BeforeTest
    public void setUp()  {
        System.out.println("Before Test");
    }

    @Test
    public void getDeviceHealth() throws MalformedURLException, InterruptedException, UnirestException {
        String url = "https://lisbon.experitest.com/api/v1/devices";
        String accessKey =
                "eyJhbGciOiJIUzI1NiJ9.eyJ4cC51Ijo0MiwieHAucCI6MywieHAubSI6MTcwMTAwMjYwNzIxMiwiZXhwIjoyMDE2MzYyNjA3LCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.SMGYMR4IFha22QTlFFHg9UdyayG4kx4VcRHg7PjsYBY";
        HttpResponse<String> jsonResponse2 = Unirest.get(url).header("Authorization", "Bearer " + accessKey).asString();
        JSONObject jsonObject = new JSONObject(jsonResponse2.getBody());
        JSONArray dataArray = jsonObject.getJSONArray("data");
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            String udid = item.getString("udid");
            String deviceOS = item.getString("deviceOs");
            String status = item.getString("displayStatus");
            String DHM = item.getString("agentName");
            String deviceName = item.getString("deviceName");
            String osVersion = item.getString("osVersion");

            DesiredCapabilities dc = new DesiredCapabilities();
            dc.setCapability("digitalai:testName", "Lisbon sanity check");
            dc.setCapability("digitalai:accessKey", accessKey);
            dc.setCapability(MobileCapabilityType.UDID, udid);

            String result;
            String WiFI;
            String iOSWiFi;
            if ("iOS".equalsIgnoreCase(deviceOS) && "available".equalsIgnoreCase(status)) {
                dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
                dc.setCapability(IOSMobileCapabilityType.BUNDLE_ID, "com.apple.Preferences");
                driver = new IOSDriver<>(new URL("https://lisbon.experitest.com/wd/hub"), dc);
                driver.launchApp();
                Thread.sleep(2000);
                String pageSource = driver.getPageSource().toLowerCase();
                iOSWiFi = Stream.of("Faro", "Evora")
                        .filter(name -> pageSource.contains(name.toLowerCase()))
                        .findFirst()
                        .orElse(null);
                System.out.println("Device:" + deviceName + "-" + DHM + "-" + osVersion + "-" + iOSWiFi);
                driver.quit();
            } else if ("Android".equalsIgnoreCase(deviceOS) && "available".equalsIgnoreCase(status)) {
                dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
                dc.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, "com.android.settings");
                dc.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, "com.android.settings.Settings");
                driver = new AndroidDriver<>(new URL("https://lisbon.experitest.com/wd/hub"), dc);
                seetest = new SeeTestClient(driver);
                result = seetest.run("adb -s " + udid + " shell dumpsys wifi | grep \"SSID\"");
                String[] parts = result.split(",");
                WiFI = parts[0].trim();
                System.out.println("Device:" + deviceName + "-" + DHM + "-" + osVersion + "-" + WiFI);
                driver.quit();
            }
        }
    }

    @AfterTest
    public void tearDown () {
        System.out.println("Report URL: ");
//            driver.quit();
    }
}