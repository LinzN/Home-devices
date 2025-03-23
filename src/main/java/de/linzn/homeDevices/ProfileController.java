package de.linzn.homeDevices;

import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.stem.stemSystem.STEMSystemApp;

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
            STEMSystemApp.LOGGER.CONFIG("Changing SmartHomeProfile to " + smartHomeProfile.name());

            for (MqttDevice mqttDevice : this.plugin.getDeviceManager().getAllDevices()) {
                if (!mqttDevice.deviceProfile.changeSmartProfile()) {
                    success = false;
                    STEMSystemApp.LOGGER.ERROR("Error while changing SmartHomeProfile to " + smartHomeProfile.name());
                } else {
                    STEMSystemApp.LOGGER.CONFIG("Success changing SmartHomeProfile to " + smartHomeProfile.name());
                }
            }
        }
        return success;
    }

    public boolean requestProfileChange(SmartHomeProfile profile) {
        return changeSmartHomeProfile(profile);
    }

}
