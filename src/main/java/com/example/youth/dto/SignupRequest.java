package com.example.youth.dto;

public class SignupRequest {

    private String idToken;
    private String nickname;
    private String appVersion;
    private String deviceId;

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
