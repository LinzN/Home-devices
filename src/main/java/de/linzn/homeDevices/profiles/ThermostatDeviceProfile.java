package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.other.ZigbeeThermostatDevice;
import de.linzn.simplyConfiguration.FileConfiguration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ThermostatDeviceProfile extends DeviceProfile {

    private static final DateTimeFormatter TIMER_FORMAT = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");
    private static final double FALLBACK_TEMPERATURE = 15;

    private final DailyTimer<Double> thermostatTimer;

    public ThermostatDeviceProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
        this.thermostatTimer = new DailyTimer<>(this.getPlugin(), "thermostat " + name, this::applyTemperature);
    }

    @Override
    public void loadProfile() {
        this.loadThermostatTimer();
    }

    @Override
    public void runProfile() {
        this.thermostatTimer.start();
    }

    @Override
    public void stopProfile() {
        this.thermostatTimer.stop();
    }

    @Override
    public boolean changeSmartProfile() {
        try {
            this.loadThermostatTimer();
            return true;
        } catch (Exception e) {
            this.getPlugin().getPluginLogger().error("Invalid thermostat settings for " + this.getName(), e);
            return false;
        }
    }

    private void applyTemperature(LocalTime time, Double value) {
        if (this.mqttDevice instanceof ZigbeeThermostatDevice zigbeeThermostatDevice) {
            this.getPlugin().getPluginLogger().info("Set thermostat " + this.getName() + " value to: " + value);
            zigbeeThermostatDevice.setTemperature(value);
        }
    }

    private void loadThermostatTimer() {
        SmartHomeProfile smartHomeProfile = this.getPlugin().getProfileController().getCurrentProfile();
        String settingsPath;
        FileConfiguration config;
        if (this.getLoadedConfig().contains("thermostat." + smartHomeProfile.name())) {
            settingsPath = "thermostat." + smartHomeProfile.name();
            config = this.getLoadedConfig();
            this.getPlugin().getPluginLogger().debug("Load custom thermostat settings CUSTOM:" + smartHomeProfile.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        } else if (getDefaultConfig().contains("thermostat." + smartHomeProfile.name())) {
            settingsPath = "thermostat." + smartHomeProfile.name();
            config = getDefaultConfig();
            this.getPlugin().getPluginLogger().debug("Load default thermostat settings DEFAULT:" + smartHomeProfile.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        } else if (this.getLoadedConfig().contains("thermostat." + SmartHomeProfile.DEFAULT.name())) {
            settingsPath = "thermostat." + SmartHomeProfile.DEFAULT.name();
            config = getLoadedConfig();
            this.getPlugin().getPluginLogger().warning("Load custom default thermostat settings CUSTOM:" + SmartHomeProfile.DEFAULT.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        } else {
            config = getDefaultConfig();
            settingsPath = "thermostat." + SmartHomeProfile.DEFAULT.name();
            this.getPlugin().getPluginLogger().warning("Load default fallback thermostat settings DEFAULT:" + SmartHomeProfile.DEFAULT.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        }

        List<Map.Entry<LocalTime, Double>> entries = new ArrayList<>();
        if (config.get(settingsPath) instanceof Map<?, ?> objectMap) {
            for (Object key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(config.getString(settingsPath + "." + key + ".time"), TIMER_FORMAT);
                double value = config.getDouble(settingsPath + "." + key + ".value");
                this.getPlugin().getPluginLogger().debug("Add thermostat timer for hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " time: " + localTime + " value: " + value);
                entries.add(Map.entry(localTime, value));
            }
        } else {
            this.getPlugin().getPluginLogger().warning("No thermostat timers configured for " + this.getName());
        }
        this.thermostatTimer.setEntries(entries);
    }

    /**
     * @return the temperature that should currently be active according to the timer list
     */
    public double getCurrentConfigTemperature() {
        Double value = this.thermostatTimer.getCurrentValue();
        return value != null ? value : FALLBACK_TEMPERATURE;
    }
}
