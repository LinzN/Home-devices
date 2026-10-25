package de.linzn.homeDevices;

import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;

import java.util.Objects;

public class ProfileController {

    public final HomeDevicesPlugin plugin;
    private volatile SmartHomeProfile currentSmartHomeProfile;

    public ProfileController(HomeDevicesPlugin homeDevicesPlugin) {
        this.plugin = homeDevicesPlugin;
        this.currentSmartHomeProfile = SmartHomeProfile.DEFAULT;
    }

    public SmartHomeProfile getCurrentProfile() {
        return currentSmartHomeProfile;
    }

    /* synchronized: the keyboard device, the web api and StemLink can request a change at the same time */
    private synchronized boolean changeSmartHomeProfile(SmartHomeProfile smartHomeProfile) {
        boolean success = true;
        if (this.currentSmartHomeProfile != smartHomeProfile) {
            this.currentSmartHomeProfile = smartHomeProfile;
            this.plugin.getPluginLogger().info("Changing SmartHomeProfile to " + smartHomeProfile.name());
            for (MqttDevice mqttDevice : this.plugin.getDeviceManager().getAllDevices()) {
                boolean changed;
                try {
                    changed = mqttDevice.getDeviceProfile().changeSmartProfile();
                } catch (Exception e) {
                    this.plugin.getPluginLogger().error("Error while changing SmartHomeProfile of " + mqttDevice.getConfigName(), e);
                    changed = false;
                }
                if (!changed) {
                    success = false;
                    this.plugin.getPluginLogger().error("Error while changing SmartHomeProfile to " + smartHomeProfile.name() + " for " + mqttDevice.getConfigName());
                } else {
                    this.plugin.getPluginLogger().debug("Success changing SmartHomeProfile to " + smartHomeProfile.name() + " for " + mqttDevice.getConfigName());
                }
            }
        }
        return success;
    }

    public boolean requestProfileChange(SmartHomeProfile profile) {
        return changeSmartHomeProfile(Objects.requireNonNull(profile, "profile"));
    }
}
