package de.linzn.homeDevices.devices.exceptions;

import java.io.IOException;

public class DeviceNotInitializedException extends IOException {
    public DeviceNotInitializedException(){
        super("Device has not been initialized yet!");
    }
}
