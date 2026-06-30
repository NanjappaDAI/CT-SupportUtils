package tests;

import com.experitest.client.Client;
import com.experitest.client.GridClient;
import com.experitest.client.Utils;
import com.experitest.client.log.Level;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class SeeTestClientSanityWebTestsiOS extends BaseWebSanityTest {

    @Test
    public void runTests() throws UnirestException {
        List<Object[]> devices = fetchDevices("iOS");
        System.out.println("Total Available iOS devices : " + devices.size());
        GridClient grid = new GridClient(accessKey, cloudURL);
        grid.setLogger(Utils.initDefaultLogger(Level.OFF));

        for (int i = 0; i < devices.size(); i++) {
            Client client = null;
            googleValidated = false;
            DeviceContext ctx = buildDeviceContext(devices.get(i), "iOS-Web-Test - ");
            System.out.println((i + 1) + ". Running test on iOS device : " + ctx.udid);
            try {
                client = grid.lockDeviceForExecution(ctx.testName, ctx.deviceQuery, 3, TimeUnit.MINUTES.toMillis(5));
                client.setReporter("xml", "", ctx.testName);
                iOSWebTest(client, ctx);
                runApiValidation(client, ctx.deviceId);
                client.setReportStatus(googleValidated ? "PASSED" : "FAILED", "GoogleValidated=" + googleValidated);
            } catch (Exception e) {
                String error = "Test failed for device " + ctx.udid + " : " + e.getMessage();
                failedTestsList.add(error);
                if (client != null) {
                    try {
                        client.setReportStatus("FAILED", e.getMessage());
                    } catch (Exception ignored) {
                    }
                }
            } finally {
                releaseClient(client, ctx);            }
        }
        printFailedTests("ios");
    }

    private void iOSWebTest(Client client, DeviceContext ctx) {
        try {
            client.launch("safari:http://google.com", true, false);
            client.sleep(4000);
            client.waitForElement("NATIVE", "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0, 4000);
            if (client.isElementFound("NATIVE", "//*[@class='UIAView' and contains(@name,'Can') and contains(@name,'Open Page')]", 0)) {
                client.report("Can't open page element detected", false);
            } else if (client.isElementFound("WEB", "//*[@text='Sign in']", 0)) {
                client.report("Google home page seen", true);
                googleValidated = true;
            } else if (client.isElementFound("WEB", "//*[@text='Got it']", 0)) {
                client.report("Google home page seen", true);
                googleValidated = true;
            } else if (client.isElementFound("WEB", "//*[@aria-label='Google' or @text='Sign in']", 0))
            {
                client.report("Google home page seen", true);
                googleValidated = true;
            } else {
                client.report("Google home page NOT seen", false);
                googleValidated = false;
            }
        } catch (Exception e) {
            int idx = e.getMessage().lastIndexOf(":");
            String msg = (idx != -1) ? e.getMessage().substring(idx + 1) : e.getMessage();
            failedTestsList.add(ctx.testName + " - " + msg.replaceAll("\\s+", " ").trim());
        }
    }
}
