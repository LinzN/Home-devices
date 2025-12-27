package de.linzn.homeDevices;

import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.stem.STEMApp;

public class ProfileController {

    public HomeDevicesPlugin plugin;
    private SmartHomeProfile currentSmartHomeProfile;

    public ProfileController(HomeDevicesPlugin homeDevicesPlugin) {
        this.plugin = homeDevicesPlugin;
        this.currentSmartHomeProfile = SmartHomeProfile.DEFAULT;
    }

    public SmartHomeProfile getCurrentProfile() {
        return currentSmartHomeProfile;
    }

    private boolean changeSmartHomeProfile(SmartHomeProfile smartHomeProfile) {
        boolean success = true;
        if (this.currentSmartHomeProfile != smartHomeProfile) {
            this.currentSmartHomeProfile = smartHomeProfile;
            STEMApp.LOGGER.CONFIG("Changing SmartHomeProfile to " + smartHomeProfile.name());

            for (MqttDevice mqttDevice : this.plugin.getDeviceManager().getAllDevices()) {
                if (!mqttDevice.deviceProfile.changeSmartProfile()) {
                    success = false;
                    STEMApp.LOGGER.ERROR("Error while changing SmartHomeProfile to " + smartHomeProfile.name());
                } else {
                    STEMApp.LOGGER.CONFIG("Success changing SmartHomeProfile to " + smartHomeProfile.name());
                }
            }
        }
        return success;
    }

    public boolean requestProfileChange(SmartHomeProfile profile) {
        return changeSmartHomeProfile(profile);
    }

}
