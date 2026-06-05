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

        private static final String accessKey = System.getenv("KEY_TO_REBECCA");
    private static final String cloudURL = "https://uscloud.experitest.com";
//    private static final String accessKey = "";
//    private static final String accessKey = "";
//        private static final String cloudURL = "https://lisbon.experitest.com";
    private static final List<String> failedTestsList = new ArrayList<>();
    private boolean GoogleValidated = false;
    private static String testname = null;

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
            if ("Available".equalsIgnoreCase(status)
//                    && osVersion.startsWith("18")
            )
            {
                deviceData.add(new Object[]{ udid, os, deviceId, osVersion });
            }
        }
        System.out.println("Total Available iOS devices : " + deviceData.size());
        GridClient grid = new GridClient(accessKey, cloudURL);
        grid.setLogger(Utils.initDefaultLogger(Level.OFF));

        for (int i = 0; i < deviceData.size(); i++) {
            Client client = null;
                Object[] device = deviceData.get(i);
                String udid = (String) device[0];
//                String udid = "00008132-000C29393483001C";
                String deviceId = (String) device[2];
               String osVersion = (String) device[3];
                String deviceQuery = "@serialnumber='" + udid + "'";
                testname = "ClientWebTest - " + udid + " - " + osVersion;
                System.out.println((i + 1) + ". Running test on iOS device : " + udid);
            try {
                client = grid.lockDeviceForExecution(    testname, deviceQuery, 3, TimeUnit.MINUTES.toMillis(5));
                client.setReporter("xml", "", testname);
                iOSWebTest(client, testname);

                String apiUrl = cloudURL + "/api/v1/devices/" + deviceId + "/http-request";
                HttpResponse<String> apiResponse =
                        Unirest.post(apiUrl)
                                .header("Authorization", "Bearer " + accessKey)
                                .header("content-type", "application/json")
                                .body("{\"url\":\"https://text.npr.org\"}")
                                .asString();

                boolean ok = apiResponse.getStatus() == 200;
                client.report(ok ? "API validation returned 200 OK" : "API validation failed | HTTP " + apiResponse.getStatus(), ok);
                String testStatus = GoogleValidated ? "PASSED" : "FAILED";
                String msg = "Validation " + testStatus + " | Google: " + (GoogleValidated ? "OK" : "FAIL");
                client.setReportStatus(testStatus, msg);

            } catch (Exception e) {
                System.out.println("Test failed for device: " + e.getMessage());
                e.printStackTrace();
                if (client != null) {
                    try {
                        client.setReportStatus("FAILED", "Exception occurred: " + e.getMessage()
                        );
                    } catch (Exception ignored) {
                    }
                }
            } finally {
                if (client != null) {
                    try {
                        client.generateReport(false);
                        client.releaseClient();
                        System.out.println("Released device");
                    } catch (Exception e) {
                        System.out.println("Failed to release client: " + e.getMessage());
                        String log = "Exception occurred in : " + e.getMessage();
                        failedTestsList.add(testname + log.substring(log.indexOf(":") + 1).trim());
                    }
                }
            }
        }
        printFailedTests();
    }

    private void iOSWebTest(Client client, String testName) {
        try {
            client.launch("safari:http://google.com", true, false);
            client.sleep(4000);
            client.waitForElement("NATIVE", "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0, 4000);
            if (client.isElementFound("NATIVE", "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0)) {
                client.report("Can't open page element detected", false);
            } else if (client.isElementFound("WEB", "//*[@text='Sign in']", 0)) {
                client.report("Google home page seen", true);
                GoogleValidated = true;
            } else if (client.isElementFound("WEB", "//*[@text='Got it']", 0)) {
                client.report("Google home page seen", true);
                GoogleValidated = true;
            } else if (client.isElementFound("WEB", "//*[@aria-label='Google' or @text='Sign in']", 0))
            {
                client.report("Google home page seen", true);
                GoogleValidated = true;
            } else {
                client.report("Google home page NOT seen", false);
                GoogleValidated = false;
            }
        } catch (Exception e) {
            String log = "Exception occurred in : " + e.getMessage();
            failedTestsList.add(testname + " - " + log.substring(log.lastIndexOf(":") + 1).replaceAll("\\s+", " ").trim());
        }
    }

    public void printFailedTests() {
        System.out.println("start-here");
        failedTestsList.forEach(System.out::println);
        System.out.println("end-here");
    }
}
