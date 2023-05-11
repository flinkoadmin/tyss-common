package com.tyss.optimize.common.util;

import java.util.Arrays;
import java.util.List;

public class ProjectTypes {
    public ProjectTypes(){

    }
    public static final String WEB = "Web";
    public static final String MOBILE = "Mobile";
    public static final String WEB_AND_MOBILE = "Web & Mobile";
    public static final String WEB_SERVICES = "Web Services";
    public static final String WEB_SERVICE = "WebService";
    public static final String SALES_FORCE = "Salesforce";
    public static final String SFDC = "Sfdc";
    public static final String SFDC_MOBILE = "Sfdc & Mobile";
    public static final String SFDC_WEB = "Sfdc & Web";
    public static final List<String> listOfProjectTypes = Arrays.asList(WEB,MOBILE,WEB_AND_MOBILE,WEB_SERVICES,SALES_FORCE);

}
