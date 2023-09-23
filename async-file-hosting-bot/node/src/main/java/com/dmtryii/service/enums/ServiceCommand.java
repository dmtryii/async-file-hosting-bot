package com.dmtryii.service.enums;

public enum ServiceCommand {
    HELP("/help"),
    REGISTRATION("/registration"),
    CANCEL("/cancel"),
    START("/start");

    private final String value;

    ServiceCommand(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ServiceCommand fromValue(String v) {
        for(ServiceCommand sc: ServiceCommand.values()) {
            if(sc.value.equals(v)) return sc;
        }
        return null;
    }
}
