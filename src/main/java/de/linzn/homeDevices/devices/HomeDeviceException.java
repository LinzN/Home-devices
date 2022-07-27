package de.linzn.homeDevices.devices;

import java.io.IOException;

public class HomeDeviceException extends IOException {
    public HomeDeviceException(String msg){
        super(msg);
    }
}
