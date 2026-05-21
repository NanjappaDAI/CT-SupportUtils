package tests;

import com.experitest.client.Client;
import com.experitest.client.GridClient;
import com.experitest.client.Utils;
import com.experitest.client.log.Level;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SeeTestClientSanityWebTests {

    private static final String cloudURL = "https://uscloud.experitest.com";
    String accessKey = "aut_1_BOam4oOXuJFMnR8c3JOBJ6gDILqfBf80vvsZPO8-CHQ=";

//    private static final String cloudURL = "https://lisbon.experitest.com";
//    String accessKey = "aut_1_0mSJdlr88QCFpa-2LJ28I3wXQhpi0Brmqof-V2g7Kyw=";

    private static final Queue<String> failedTestsList = new ConcurrentLinkedQueue<>();
    private static final Queue<String> androidDeviceInfoList = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<Client> client = new ThreadLocal<>();
    private final ThreadLocal<GridClient> grid = new ThreadLocal<>();

    private boolean GoogleValidated = false;

    public SeeTestClientSanityWebTests() {
    }

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
            String osVersion = item.getString("osVersion");
            String status = item.getString("displayStatus");
            String deviceId = item.getString("id");
            String region = item.getString("region");
            String deviceName = item.getString("deviceName");
            String testNameSuffix = String.format("%s_%s_%s", udid, osVersion, region);
            if ("Available".equalsIgnoreCase(status)) {
                deviceData.add(new Object[]{ udid, os, deviceId, testNameSuffix });
            }
        }
        System.out.println("Total Available devices : " + deviceData.size());
        return deviceData.toArray(new Object[0][0]);
    }

    @Test(dataProvider = "devices")
    public void getDeviceHealth(String udid, String deviceOS, String deviceID, String testNameSuffix) {
        StringBuilder log = new StringBuilder();
        GridClient grid = new GridClient(accessKey, cloudURL);
        String testName = "Seetest Client Web Test_" + testNameSuffix;
        String deviceQuery = "@serialnumber='" + udid + "'";
        Client seetestClient = grid.lockDeviceForExecution(testName, deviceQuery, 3, TimeUnit.MINUTES.toMillis(5));
        client.set(seetestClient);
        client.get().setReporter("xml", "", testName);

        if ("iOS".equalsIgnoreCase(deviceOS)) {
            iOSWebTest();
        } else {
            androidWebTest();
        }

        String apiUrl = cloudURL + "/api/v1/devices/" + deviceID + "/http-request";
        HttpResponse<String> response;
        try {
            response = Unirest.post(apiUrl).header("Authorization", "Bearer " + accessKey)
                    .header("content-type", "application/json").body("{\n    \"url\":\"https://text.npr.org\"\n}")
                    .asString();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }

        if (response.getStatus() == 200) {
            client.get().report("API validation returned 200 OK", true);
        } else {
            client.get().report("API validation returned unexpected error | HTTP " + response.getStatus() , false);
        }

        String status = GoogleValidated ? "PASSED" : "FAILED";
        String msg = "Validation " + status + " | Google: " + (GoogleValidated ? "OK" : "FAIL");
        client.get().setReportStatus(status, msg);

        if (status.equals("FAILED")) {
            client.get().setLogger(Utils.initDefaultLogger(Level.OFF));
            String xml = client.get().getVisualDump("Web");
            Pattern pattern = Pattern.compile("text=\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(xml);
            int count = 0;
            while (matcher.find() && count < 5) {
                String text = matcher.group(1).trim();
                if (!text.isBlank()) {
                    if (log.length() > 0) {
                        log.append(" ");
                    }
                    log.append(text);
                    count++;
                }
            }
        }
        failedTestsList.add(String.valueOf(log));
    }

    private void androidWebTest() {
        client.get().launch("chrome:http://google.com/ncr", true, false);
        client.get().sleep(10000);
        client.get().waitForElement("NATIVE", "//*[contains(@name,'Can') and contains(@name,'Open Page')]", 0, 15000);
        if (client.get().isElementFound("WEB", "//*[@text='Sign in']", 0)) {
            client.get().report("Google home page seen", true);
            GoogleValidated = true;
        } else if (client.get()
                .isElementFound("NATIVE", "//*[contains(@name,'Can') and contains(@name,'Open Page')]", 0)) {
            client.get().report("Can't open page element detected", false);
        } else if (client.get().isElementFound("WEB", "//*[@text='Got it']", 0)) {
            client.get().report("Google home page seen", true);
            GoogleValidated = true;
        } else if (client.get().isElementFound("WEB", "//*[@aria-label='Google' or @text='Sign in']", 0)) {
            client.get().report("Google home page seen", true);
            GoogleValidated = true;
        } else {
            client.get().report("Google home page NOT seen", false);
        }
    }

    private void iOSWebTest() {
        client.get().launch("safari:http://google.com", true, false);
        client.get().sleep(10000);
        client.get().waitForElement("NATIVE",
                "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0, 15000);
        if (client.get().isElementFound("NATIVE",
                "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0)) {
            client.get().report("Can't open page element detected", false);
        } else if (client.get().isElementFound("WEB", "//*[@text='Sign in']", 0)) {
            client.get().report("Google home page seen", true);
            GoogleValidated = true;
        } else if (client.get().isElementFound("WEB", "//*[@text='Got it']", 0)) {
            client.get().report("Google home page seen", true);
            GoogleValidated = true;
        } else if (client.get().isElementFound("WEB", "//*[@aria-label='Google' or @text='Sign in']", 0)) {
            client.get().report("Google home page seen", true);
            GoogleValidated = true;
        } else {
            client.get().report("Google home page NOT seen", false);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        Client currentClient = client.get();
        if (currentClient != null) {
            try {
                currentClient.generateReport(false);
            } catch (Exception e) {
                System.out.println("Report generation failed: " + e.getMessage());
            }
            try {
                currentClient.releaseClient();
            } catch (Exception e) {
                System.out.println("Release client failed: " + e.getMessage());
            }
            client.remove();
        }
    }

    @AfterSuite
    public void printAllCollectedDeviceIDs() {
        System.out.println("start-here");
        failedTestsList.forEach(System.out::println);
        System.out.println("end-here");
    }
}

//        HttpResponse<String> accessKeyResponse = Unirest.get(cloudURL + "/api/v2/test-requests/key").header("Authorization", "Basic YWRtaW46QWIxMjM0NTY=").asString();
//    JSONObject json = new JSONObject(accessKeyResponse.getBody());
//    String accessKey = json.getString("key");