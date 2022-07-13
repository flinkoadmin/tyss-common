package com.tyss.optimize.common.util;

import java.util.Arrays;
import java.util.List;

public class CloudPlatforms {

    public static final List<String> web_browserStackCapabilityKeys = Arrays.asList("os:osName", "os_version:osVersion", "platform:platform", "browser_version:version");
    public static final List<String> mobile_browserStackCapabilityKeys = Arrays.asList("os_version:osVersion", "deviceName:deviceName", "platformName:platformName");
    public static final List<String> web_saucelabCapabilityKeys = Arrays.asList("os_name:osName", "os_version:osVersion", "platform:platform", "version:version");
    public static final List<String> mobile_saucelabCapabilityKeys = Arrays.asList("deviceName:deviceName", "platformName:platformName", "platformVersion:platformVersion");
    public static final List<String> web_lambdaTestCapabilityKeys = Arrays.asList("platform:platform", "version:version");
    public static final List<String> mobile_lambdaTestCapabilityKeys = Arrays.asList("deviceName:deviceName", "platformName:platformName", "platformVersion:platformVersion");
    public static final String browserStack = "browserstack";
    public static final String saucelabs = "saucelabs";
    public static final String lambdaTest = "lambdatest";
    public static String browserStackSuffix = "@hub-cloud.browserstack.com/wd/hub";
    public static String saucelabsSuffix = "@ondemand.{region}.saucelabs.com:443/wd/hub";
    public static final String lambdaTestSuffix = "@hub.lambdatest.com/wd/hub";
    public static  String webNlpName = "SetBrowserName";
    public static  String androidNlpName = "MOB_OpenMobileApplication";
    public static  String iosNlpName = "IOS_OpenMobileApplication";
    public static String localHubUrl = "http://localhost:4444/wd/hub";
    public static String splitSymbol = "-";

}
