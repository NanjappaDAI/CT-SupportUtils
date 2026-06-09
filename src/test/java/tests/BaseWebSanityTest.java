package tests;

import com.experitest.client.Client;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class BaseWebSanityTest {
    protected static final String cloudURL = "https://uscloud.experitest.com";
    protected static final String accessKey = System.getenv("KEY_TO_REBECCA");
//    protected static final String cloudURL = "https://lisbon.experitest.com";
//    protected static final String accessKey = "";
    protected final List<String> failedTestsList = new ArrayList<>();
    protected boolean googleValidated = false;

    protected List<Object[]> fetchDevices(String osFilter) throws UnirestException {

        HttpResponse<String> response = Unirest.get(cloudURL + "/api/v1/devices").header("Authorization", "Bearer " + accessKey).asString();
        JSONArray dataArray = new JSONObject(response.getBody()).getJSONArray("data");
        List<Object[]> deviceData = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.getJSONObject(i);
            String udid = item.getString("udid");
            String os = item.getString("deviceOs");
            String osVersion = item.getString("osVersion");
            String status = item.getString("displayStatus");
            String deviceId = item.getString("id");

            if ("Available".equalsIgnoreCase(status) && osFilter.equalsIgnoreCase(os))
            {
                deviceData.add(new Object[]{udid, os, deviceId, osVersion});
            }
        }
        return deviceData;
    }

    protected DeviceContext buildDeviceContext(Object[] device, String prefix) {
        String udid = (String) device[0];
        String os = (String) device[1];
        String deviceId = (String) device[2];
        String osVersion = (String) device[3];

        return new DeviceContext(
                udid,
                deviceId,
                osVersion,
                prefix
        );
    }

    protected void runApiValidation(Client client, String deviceId) throws UnirestException {

        String apiUrl = cloudURL + "/api/v1/devices/" + deviceId + "/http-request";

        HttpResponse<String> apiResponse =
                Unirest.post(apiUrl)
                        .header("Authorization", "Bearer " + accessKey)
                        .header("content-type", "application/json")
                        .body("{\"url\":\"https://text.npr.org\"}")
                        .asString();

        boolean ok = apiResponse.getStatus() == 200;

        client.report(
                ok ? "API validation returned 200 OK"
                        : "API validation failed | HTTP " + apiResponse.getStatus(),
                ok
        );
    }

    protected void releaseClient(Client client, DeviceContext ctx) {
        if (client == null) return;
        try {
            client.generateReport(false);
            client.releaseClient();
        } catch (Exception e) {
            String error = "Release failed for device " + ctx.udid + " : " + e.getMessage();
            System.out.println(error);
            failedTestsList.add(error);
        }
    }


    public void printFailedTests() {
        System.out.println("start-here");
        failedTestsList.forEach(System.out::println);
        System.out.println("end-here");
    }

    public static class DeviceContext {
        public String udid, deviceId, osVersion, deviceQuery, testName;
        public DeviceContext(String udid, String deviceId, String osVersion, String prefix) {
            this.udid = udid;
            this.deviceId = deviceId;
            this.osVersion = osVersion;
            this.deviceQuery = "@serialnumber='" + udid + "'";
            this.testName = prefix + udid + " - " + osVersion;
        }
    }
}