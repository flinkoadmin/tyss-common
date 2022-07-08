package com.tyss.optimize.common.model.auth;

import lombok.Data;

@Data
public class AccessTokenMapper {

    private String access_token;
    private String id;
    private String userName;
    private String name;
    private String licenseId;
    private String privilege;
    private String isActive;
    private boolean isValid;

}
