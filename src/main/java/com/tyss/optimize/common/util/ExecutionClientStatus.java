package com.tyss.optimize.common.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExecutionClientStatus {

    public static Map<String, List<String>> clientStatusMap = initMap();
    public static String priorityNumber = "{priority}";
    public static String licenseParallelRun = "{licenseNumber}";
    public static String currentParallelRunning = "{parallelRun}";
    public static String clientStatusCheck = "Checking for client status. Please make sure the services are up and running";
    public static String executionCancelled = "Execution is cancelled as the selected clients are not available";
    public static String clientMessageValue = "Execution is already triggered on this machine. Your execution order priority is "+ priorityNumber + ". For more details click on machine name.";
    public static String parallelRunValidation = "This license is limited to " +licenseParallelRun+" parallel runs, there are already "+ currentParallelRunning +" executions running on this license.";
    public static String clientMessageKey = "clientMessage";
    public static String clientCheck = "clientCheck";
    public static String formRequest = "Initializing...";
    private static Map<String, List<String>> initMap() {
        return Map.of("core-" + CommonConstants.Web, Arrays.asList("Client", "Sync", "Webservices", "Node"), "core-" + CommonConstants.Mobile, Arrays.asList("Client", "Sync", "Webservices", "Appium"), "core-" + CommonConstants.WebAndMobile, Arrays.asList("Client", "Sync", "Webservices", "Node", "Appium"), "core-" + CommonConstants.Webservice, Arrays.asList("Client", "Sync", "Webservices"), "core-" + CommonConstants.Android, Arrays.asList("Client", "Sync", "Webservices", "Appium"), "core-" + CommonConstants.iOS, Arrays.asList("Client", "Sync", "Webservices", "Appium"), "core-" + CommonConstants.Database, Arrays.asList("Client", "Sync", "Webservices", "Node"));
    }
    public static String status = "status";
    public static String message = "message";
}
