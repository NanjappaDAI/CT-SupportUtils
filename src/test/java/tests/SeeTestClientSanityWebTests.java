package tests;

import com.experitest.client.Client;
import com.experitest.client.GridClient;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SeeTestClientSanityWebTests {

    private static final String cloudURL = "https://uscloud.experitest.com";
    private static final Queue<String> iOSDeviceInfoList = new ConcurrentLinkedQueue<>();
    private static final Queue<String> androidDeviceInfoList = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<Client> client = new ThreadLocal<>();
    private final ThreadLocal<GridClient> grid = new ThreadLocal<>();

    String accessKey = "aut_1_BOam4oOXuJFMnR8c3JOBJ6gDILqfBf80vvsZPO8-CHQ=";


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
        for (Object[] device : deviceData) {
            System.out.println(Arrays.toString(device));
        }
        return deviceData.toArray(new Object[0][0]);
    }

    @Test(dataProvider = "devices")
    public void getDeviceHealth(String udid, String deviceOS, String deviceID, String testNameSuffix) {
        GridClient grid = new GridClient(accessKey, cloudURL);
        String testName = "Seetest Client Web Test_" + testNameSuffix;
        String deviceQuery = "@serialnumber='" + udid + "'";
        Client seetestClient = grid.lockDeviceForExecution(testName, deviceQuery, 3, TimeUnit.MINUTES.toMillis(5));
        client.set(seetestClient);
        client.get().setReporter("xml", "", testName);

        String log;
        if ("iOS".equalsIgnoreCase(deviceOS)) {
            client.get().launch("safari:http://google.com", true, false);
        } else {
            client.get().launch("chrome:http://google.com", true, false);
        }
        client.get().sleep(10000);
        if (client.get().isElementFound("WEB", "//*[@name='q' or @aria-label='Search']", 0)) {
            client.get().report("Google home page seen", true);
            boolean googleValidated = true;
        } else {
            client.get().report("Google home page NOT seen", false);
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
        client.get().report("API validation for returned Response: " + response.getStatusText(), response.getStatus() == 200);
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
        System.out.println("end-here");
    }
}

//        HttpResponse<String> accessKeyResponse = Unirest.get(cloudURL + "/api/v2/test-requests/key").header("Authorization", "Basic YWRtaW46QWIxMjM0NTY=").asString();
//    JSONObject json = new JSONObject(accessKeyResponse.getBody());
//    String accessKey = json.getString("key");