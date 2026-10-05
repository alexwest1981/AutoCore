package com.wac.autocore.model;


public enum LoadLevel {
    FREE("free"),        // 0-2 timmar
    MODERATE("moderate"),  // 3-4 timmar
    BUSY("busy"),       // 5-6 timmar
    FULL("full");          // 7+ timmar

    private final String code;

    LoadLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
