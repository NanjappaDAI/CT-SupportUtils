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
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

public class SeeTestClientSanityWebTestsiOS {

    private static final String cloudURL = "https://uscloud.experitest.com";
    private static final String accessKey = "aut_1_BOam4oOXuJFMnR8c3JOBJ6gDILqfBf80vvsZPO8-CHQ=";

    private boolean GoogleValidated = false;

    @Test
    public void runTests() throws UnirestException {
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
            if ("Available".equalsIgnoreCase(status) && "iOS".equalsIgnoreCase(os)) {
                deviceData.add(new Object[]{ udid, os, deviceId, osVersion });
            }
        }
        System.out.println("Total Available devices : " + deviceData.size());
        GridClient grid = new GridClient(accessKey, cloudURL);
        grid.setLogger(Utils.initDefaultLogger(Level.OFF));

        for (int i = 0; i < deviceData.size(); i++) {
            Object[] device = deviceData.get(i);
            String udid = (String) device[0];
            String deviceQuery = "@serialnumber='" + udid + "'";

            System.out.println((i + 1) + ". Running test on device : " + udid);

            Client client = grid.lockDeviceForExecution("ClientWebTest - " + udid, deviceQuery, 3,
                    TimeUnit.MINUTES.toMillis(5));
            client.setReporter("xml", "", "ClientWebTest - " + udid);
            iOSWebTest(client);

            String apiUrl = cloudURL + "/api/v1/devices/" + "/http-request";
            HttpResponse<String> apiResponse = Unirest.post(apiUrl).header("Authorization", "Bearer " + accessKey)
                    .header("content-type", "application/json").body("{\"url\":\"https://text.npr.org\"}").asString();
            boolean ok = apiResponse.getStatus() == 200;
            client.report(
                    ok ? "API validation returned 200 OK" : "API validation failed | HTTP " + apiResponse.getStatus(),
                    ok);

            String testStatus = GoogleValidated ? "PASSED" : "FAILED";
            String msg = "Validation " + testStatus + " | Google: " + (GoogleValidated ? "OK" : "FAIL");
            client.setReportStatus(testStatus, msg);
            client.generateReport(false);
            client.releaseClient();
            System.out.println("Released device : " + udid);
        }
    }

    private void iOSWebTest(Client client) {
        try {
            client.launch("safari:http://google.com", true, false);
            client.sleep(4000);
            client.waitForElement("NATIVE",
                    "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0, 4000);
            if (client.isElementFound("NATIVE",
                    "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0)) {
                client.report("Can't open page element detected", false);
            } else if (client.isElementFound("WEB", "//*[@text='Sign in']", 0)) {
                client.report("Google home page seen", true);
                GoogleValidated = true;
            } else if (client.isElementFound("WEB", "//*[@text='Got it']", 0)) {
                client.report("Google home page seen", true);
                GoogleValidated = true;
            } else if (client.isElementFound("WEB", "//*[@aria-label='Google' or @text='Sign in']", 0)) {
                client.report("Google home page seen", true);
                GoogleValidated = true;
            } else {
                client.report("Google home page NOT seen", false);
                GoogleValidated = false;
            }
        } catch (Exception e) {
            String log = "Exception occurred in : " + e.getMessage();
        }
    }
}
