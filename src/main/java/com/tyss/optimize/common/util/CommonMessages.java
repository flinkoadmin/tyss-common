package com.tyss.optimize.common.util;

public class CommonMessages {

    public static final String ONLY_ADMIN_AND_SUPER_ADMIN_ACCESS = "Only Admins and super admins have access to perform this action";

    public static final String getPasswordWarnings() {
        StringBuffer passwordWarnings = new StringBuffer();

        passwordWarnings.append(" * Should contain at least 8 characters and at most 20 characters. ");
        passwordWarnings.append(" * Should contain at least one digit. ");
        passwordWarnings.append(" * Should contain at least one upper case alphabet. ");
        passwordWarnings.append(" * Should contain at least one lower case alphabet. ");
        passwordWarnings.append(" * Should contain at least one special character which includes !@#$%&*()+=^. ");
        passwordWarnings.append(" * Should not contain any white space. ");

        return passwordWarnings.toString();
    }
    
    public static final String INVALID_STORAGE = "Invalid Storage";
    public static final String INVALID_PARALLELRUNS_LIMIT = "Maximum number of parallelRuns can be 25 and storage can be 100 GB";
    public static final String INVALID_PARALLELRUNS = "Invalid parallelRuns";
}
