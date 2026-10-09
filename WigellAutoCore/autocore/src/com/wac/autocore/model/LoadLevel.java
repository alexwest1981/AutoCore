package com.wac.autocore.model;

public enum LoadLevel {
    FREE("free"),        // 0-2 hours
    MODERATE("moderate"),  // 3-4 hours
    BUSY("busy"),       // 5-6 hours
    FULL("full");          // 7+ hours

    private final String code;

    LoadLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
