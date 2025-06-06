package tests;

import com.experitest.appium.SeeTestClient;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CheckLisbonDevicesTest {

    private static final String accessKey = "aut_1_az8i6BpWyjC92T6DNonJc1fTzKQ_SzJjtX-yuwnJq0w=";
    //    private static final String accessKey= "eyJhbGciOiJIUzI1NiJ9.eyJ4cC51Ijo0MiwieHAucCI6MywieHAubSI6MTcwMTAwMjYwNzIxMiwiZXhwIjoyMDE2MzYyNjA3LCJpc3MiOiJjb20uZXhwZXJpdGVzdCJ9.SMGYMR4IFha22QTlFFHg9UdyayG4kx4VcRHg7PjsYBY";
    private static final String cloudURL = "https://lisbon.experitest.com";
    private static final Queue<String> deviceInfoList = new ConcurrentLinkedQueue<>();
    private static ThreadLocal<AppiumDriver> driver = new ThreadLocal<>();
    protected SeeTestClient seetest = null;

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

//            if ("Available".equalsIgnoreCase(status) && "iOS".equalsIgnoreCase(os)) {
//                deviceData.add(new Object[]{ udid, os, deviceId, DHM, deviceName });
//            }
        }

        return deviceData.toArray(new Object[0][0]);
    }

    // Parallel Test Execution
    @Test(dataProvider = "devices")
    public void getDeviceHealth(String udid, String deviceOS, String deviceID, String DHM, String deviceName)
            throws MalformedURLException, InterruptedException, UnirestException {
        DesiredCapabilities dc = new DesiredCapabilities();
        dc.setCapability("digitalai:testName", "Lisbon sanity check");
        dc.setCapability("digitalai:accessKey", accessKey);
        dc.setCapability(MobileCapabilityType.UDID, udid);
        System.out.println("Thread " + Thread.currentThread().getId() + " -> Device: " + udid);
        // Release device
//        Unirest.post(cloudURL + "/" + deviceID + "/api/v1/devices/release").header("Authorization", "Bearer " + accessKey).asString();
        String deviceLanguage;
        String WiFI;
        if ("iOS".equalsIgnoreCase(deviceOS)) {
            dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
            dc.setCapability("bundleId", "com.apple.Preferences");
            dc.setCapability("appiumVersion", "2.16.2");
            driver.set(new IOSDriver<>(new URL(cloudURL + "/wd/hub"), dc));
            driver.get().executeScript("mobile: terminateApp", ImmutableMap.of("bundleId", "com.apple.Preferences"));

            Thread.sleep(1000);
            driver.get().executeScript("mobile: activateApp", ImmutableMap.of("bundleId", "com.apple.Preferences"));
            Thread.sleep(3000);
            String pageSource = driver.get().getPageSource().toLowerCase();
            if (pageSource.toLowerCase().contains("id=\"settings\"")) {
                deviceLanguage = "English";
            } else {
                deviceLanguage = "NOT ENGLISH";
            }
            String iOSWiFi = Stream.of("Faro", "Evora").filter(name -> pageSource.contains(name.toLowerCase())).findFirst().orElse(null);

            if (DHM.toLowerCase().contains("porto") && iOSWiFi != null && iOSWiFi.toLowerCase().contains("evora")) {WiFI = "YES";}
            else if (DHM.toLowerCase().contains("elvas") && iOSWiFi != null && iOSWiFi.toLowerCase().contains("faro")) {WiFI = "YES";}
            else if (DHM.toLowerCase().contains("sintra") && iOSWiFi != null && iOSWiFi.toLowerCase().contains("faro")) {WiFI = "YES";}
            else {WiFI = "WRONG WiFi";}
            String log = "<tr>" + "<td>" + deviceName + "</td>" + "<td>" + WiFI + "</td>" + "<td>" + deviceLanguage + "</td>" + "</tr>";
            deviceInfoList.add(log);
            driver.get().quit();

        } else if ("Android".equalsIgnoreCase(deviceOS)) {
            dc.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
            dc.setCapability("appPackage", "com.android.settings");
            dc.setCapability("appActivity", "com.android.settings.Settings");
            dc.setCapability("appiumVersion", "2.16.2");
            driver.set(new AndroidDriver<>(new URL(cloudURL + "/wd/hub"), dc));

            String result = (String) driver.get().executeScript("mobile: shell", Map.of("command", "dumpsys", "args", List.of("wifi")));
            String ssid = Arrays.stream(result.split("\n")).map(String::trim).filter(line -> line.contains("SSID")).findFirst().orElse("SSID not found");

            Thread.sleep(1500);

            result = (String) driver.get().executeScript("mobile: shell", Map.of("command", "getprop", "args", List.of()));
            deviceLanguage = Arrays.stream(result.split("\n")).map(String::trim).filter(line -> line.toLowerCase().contains("persist.sys.locale")).findFirst().orElse("Not found");


            if (DHM.toLowerCase().contains("porto") && ssid.toLowerCase().contains("evora")) {WiFI = "YES";}
            else if (DHM.toLowerCase().contains("elvas") && ssid.toLowerCase().contains("faro")) {WiFI = "YES";}
            else if (DHM.toLowerCase().contains("sintra") && ssid.toLowerCase().contains("faro")) {WiFI = "YES";}
            else {WiFI = "NO";}

            String log = "<tr>" + "<td>" + deviceName + "</td>" + "<td>" + WiFI + "</td>" + "<td>" + deviceLanguage + "</td>" + "</tr>";
            deviceInfoList.add(log);
            driver.get().quit();
        }
    }

    @AfterSuite
    public void printAllCollectedDeviceIDs() {
        System.out.println("start-here");
        System.out.println("<html><body><table border=1>");
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss"));
        System.out.println("<tr><td> Lisbon cloud monitoring report </td><td>" + dateTime + "</td></tr>");
        System.out.println("<tr>" + "<td> Device Name </td>" + "<td> Correct WiFi? </td>" + "<td> Device Language </td>" + "</tr>");
        deviceInfoList.forEach(System.out::println);
        System.out.println("</table></body></html>");
        System.out.println("end-here");
    }

    @AfterTest
    public void tearDown() {
        System.out.println("Report URL (if needed)");
    }
}
