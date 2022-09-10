package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.linzn.simplyConfiguration.provider.YamlConfiguration;
import de.stem.stemSystem.STEMSystemApp;

import java.io.File;

public abstract class DeviceProfile {
    private final MqttDevice mqttDevice;
    private final FileConfiguration profileConfig;
    private final FileConfiguration defaultProfileConfig;


    public DeviceProfile(MqttDevice mqttDevice) {
        this.mqttDevice = mqttDevice;
        File defaultFile = new File(HomeDevicesPlugin.homeDevicesPlugin.getDataFolder(), "profiles/default.yml");
        this.defaultProfileConfig = YamlConfiguration.loadConfiguration(defaultFile);

        if (!defaultFile.exists()) {
            this.defaultProfileConfig.save();
        }

        File profileFile = new File(HomeDevicesPlugin.homeDevicesPlugin.getDataFolder(), "profiles/" + mqttDevice.configName + ".yml");
        if (profileFile.exists()) {
            this.profileConfig = YamlConfiguration.loadConfiguration(profileFile);
            STEMSystemApp.LOGGER.CONFIG("Loading profile config for hardId " + mqttDevice.getDeviceHardAddress() + " configName " + mqttDevice.getConfigName());
        } else {
            this.profileConfig = null;
            STEMSystemApp.LOGGER.CONFIG("Using default config for hardId " + mqttDevice.getDeviceHardAddress() + " configName " + mqttDevice.getConfigName());
        }
    }

    public MqttDevice getMqttDevice() {
        return mqttDevice;
    }

    public FileConfiguration getDefaultConfig() {
        return this.defaultProfileConfig;
    }

    public FileConfiguration getLoadedConfig() {
        if (profileConfig != null) {
            return profileConfig;
        } else {
            return this.defaultProfileConfig;
        }
    }

    public boolean hasOwnConfig() {
        return profileConfig != null;
    }

    public abstract void loadProfile();

    public abstract void runProfile();
}
