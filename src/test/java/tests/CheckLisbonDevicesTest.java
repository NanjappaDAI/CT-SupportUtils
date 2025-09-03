package tests;

import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CheckLisbonDevicesTest {

        private static final String accessKey = "eyJhbGciOiJIUzI1NiJ9.eyJ4cC51Ijo0MiwieHAucCI6MywieHAubSI6MTcwMTAwMjYwNzIxMiwiZXhwIjoyMDE2MzYyNjA3LCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.SMGYMR4IFha22QTlFFHg9UdyayG4kx4VcRHg7PjsYBY";
//    private static final String accessKey = System.getenv("KEY_TO_REBECCA");
    private static final String cloudURL = "https://lisbon.experitest.com";
    private static final Queue<String> iOSDeviceInfoList = new ConcurrentLinkedQueue<>();
    private static final Queue<String> androidDeviceInfoList = new ConcurrentLinkedQueue<>();
    private static ThreadLocal<AppiumDriver> driver = new ThreadLocal<>();
//    protected SeeTestClient seetest = null;

    // Data Provider for parallel execution
    @DataProvider(name = "devices", parallel = true)
    public Object[][] provideDevices() throws Exception {

        HttpResponse<String> response =
                Unirest.get(cloudURL + "/api/v1/devices").header("Authorization", "Bearer " + accessKey).asString();
        JSONArray dataArray = new JSONObject(response.getBody()).getJSONArray("data");
        List<Object[]> deviceData = new ArrayList<>();

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            String udid = item.getString("udid");
            String os = item.getString("deviceOs");
            String status = item.getString("displayStatus");
            String deviceId = item.getString("id");
            String DHM = item.getString("agentName");
            String deviceName = item.getString("deviceName");

            if ("Available".equalsIgnoreCase(status) || "In Use".equalsIgnoreCase(status)) {
                deviceData.add(new Object[]{ udid, os, deviceId, DHM, deviceName });
            }

            if ("In Use".equalsIgnoreCase(status)) {
                Unirest.post(cloudURL + "/" + deviceId + "/api/v1/devices/release")
                        .header("Authorization", "Bearer " + accessKey).asString();
            }

//            if ("Available".equalsIgnoreCase(status) && "Android".equalsIgnoreCase(os)) {
//                deviceData.add(new Object[]{ udid, os, deviceId, DHM, deviceName });
//            }
        }

        return deviceData.toArray(new Object[0][0]);
    }

    // Parallel Test Execution
    @Test(dataProvider = "devices")
    public void getDeviceHealth(String udid, String deviceOS, String deviceID, String DHM, String deviceName) throws MalformedURLException, InterruptedException {
        DesiredCapabilities dc = new DesiredCapabilities();
        dc.setCapability("digitalai:testName", "2.19.0 sanity check");
        dc.setCapability("digitalai:accessKey", accessKey);
        dc.setCapability("newSessionWaitTimeout", 180);
        dc.setCapability("newCommandTimeout", 120);
        dc.setCapability(MobileCapabilityType.UDID, udid);
        System.out.println("Thread " + Thread.currentThread().getId() + " -> Device: " + udid);

        String deviceLanguage ="";
        String WiFI = null;
        String log;


        if ("iOS".equalsIgnoreCase(deviceOS)) {
            try {
                dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
                dc.setCapability("bundleId", "com.apple.Preferences");
                dc.setCapability("appiumVersion", "2.19.0");
                driver.set(new IOSDriver<>(new URL(cloudURL + "/wd/hub"), dc));
                driver.get()
                        .executeScript("mobile: terminateApp", ImmutableMap.of("bundleId", "com.apple.Preferences"));
                Thread.sleep(2000);
                driver.get().executeScript("mobile: activateApp", ImmutableMap.of("bundleId", "com.apple.Preferences"));
                Thread.sleep(2000);
                deviceLanguage = driver.get().findElement(By.xpath("//*[@type='XCUIElementTypeApplication']"))
                        .getAttribute("label");
                List<WebElement> elements = driver.get()
                        .findElements(By.xpath("//*[@label='Wi-Fi']/following-sibling::XCUIElementTypeStaticText"));
                WiFI = elements.isEmpty() ? "N/A" : elements.get(0).getAttribute("label");
                driver.get().quit();
            } catch (Exception e) {
                deviceLanguage = e.getClass().getSimpleName();
            }
            log = "<tr>" + "<td>" + deviceName + "</td>" + "<td>" + DHM.split("-")[0] + "</td>" + "<td>" + WiFI
                    + "</td>" + "<td>" + deviceLanguage + "</td>" + "</tr>";
            iOSDeviceInfoList.add(log);

        } else if ("Android".equalsIgnoreCase(deviceOS)) {
            String ssid = null;
            try {
                dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
                dc.setCapability("appPackage", "com.android.settings");
                dc.setCapability("appActivity", "com.android.settings.Settings");
                dc.setCapability("appiumVersion", "2.19.0");
                driver.set(new AndroidDriver<>(new URL(cloudURL + "/wd/hub"), dc));

                String result = (String) driver.get()
                        .executeScript("mobile: shell", Map.of("command", "dumpsys", "args", List.of("wifi")));
                ssid = Arrays.stream(result.split("\n")).map(String::trim)
                        .filter(line -> line.contains("mWifiInfo")).findFirst().orElse("SSID not found");
                Thread.sleep(1500);
                result = (String) driver.get()
                        .executeScript("mobile: shell", Map.of("command", "getprop", "args", List.of()));
                deviceLanguage = Arrays.stream(result.split("\n")).map(String::trim)
                        .filter(line -> line.toLowerCase().contains("persist.sys.locale")).findFirst()
                        .orElse("Not found");
                driver.get().quit();
            } catch (Exception e) {
                deviceLanguage = e.getClass().getSimpleName();
            }
            try {
                 log =
                        "<tr>" + "<td>" + deviceName + "</td>" + "<td>" + DHM.split("-")[0] + "</td>" + "<td>"
                                + ssid.split(
                                " ")[2].trim().replace("\"", "").trim() + "</td>" + "<td>" + deviceLanguage + "</td>"
                                + "</tr>";
                androidDeviceInfoList.add(log);
            } catch (Exception e) {
                log = "<tr>" + "<td>" + deviceName + "</td>" + "<td> </td>" + "<td> </td>" + "<td> </td>" + e.getClass().getSimpleName() + "</tr>";
                androidDeviceInfoList.add(log);
            }
        }
    }

    @AfterSuite
    public void printAllCollectedDeviceIDs() {
        System.out.println("start-here");
        System.out.println("<html><body><table border=1>");
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));
        System.out.println("<tr><td> Lisbon cloud monitoring report </td><td>" + dateTime + "</td></tr>");
        System.out.println("<tr>" + "<td> Device Name </td>" + "<td> DHM </td>" + "<td> Correct WiFi? </td>"
                + "<td> Device Language </td>" + "</tr>");
        List<String> sortedListAndroid = new ArrayList<>(androidDeviceInfoList);
        Collections.sort(sortedListAndroid); // or use a custom Comparator
        sortedListAndroid.forEach(System.out::println);

//        androidDeviceInfoList.forEach(System.out::println);
        List<String> sortedListiOS = new ArrayList<>(iOSDeviceInfoList);
        Collections.sort(sortedListiOS); // or use a custom Comparator
        sortedListiOS.forEach(System.out::println);

//        iOSDeviceInfoList.forEach(System.out::println);
        System.out.println("</table></body></html>");
        System.out.println("end-here");
    }

    @AfterTest
    public void tearDown() {
        System.out.println("Report URL (if needed)");
    }
}
