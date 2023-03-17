package de.linzn.homeDevices.devices.enums;

import java.util.ArrayList;
import java.util.List;

public enum SmartHomeProfile {
    DEFAULT, AUTOMATIC, HOMEOFFICE, HOLYDAY, STANDBY;

    public static boolean has(String value) {
        for (SmartHomeProfile smartHomeProfile : SmartHomeProfile.values()) {
            if (smartHomeProfile.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> valuesToString() {
        List<String> values = new ArrayList<>();
        for (SmartHomeProfile smartHomeProfile : SmartHomeProfile.values()) {
            values.add(smartHomeProfile.name());
        }
        return values;
    }
}
