package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.other.ZigbeeThermostatDevice;
import de.linzn.openJL.pairs.Pair;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.stem.stemSystem.STEMSystemApp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ThermostatDeviceProfile extends DeviceProfile {

    private final long thermostatOffSet = 200;
    private LinkedList<Pair<LocalTime, Double>> thermostatTimerList;


    public ThermostatDeviceProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
    }

    @Override
    public void loadProfile() {
        this.loadThermostatTimer();
    }

    @Override
    public void runProfile() {
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, this::run, 2, TimeUnit.SECONDS);
    }

    @Override
    public boolean changeSmartProfile() {
        loadThermostatTimer();
        return true;
    }

    private void loadThermostatTimer() {
        this.thermostatTimerList = new LinkedList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

        SmartHomeProfile smartHomeProfile = HomeDevicesPlugin.homeDevicesPlugin.getCurrentProfile();

        String settingsPath;
        FileConfiguration config;

        if (this.getLoadedConfig().contains("thermostat." + smartHomeProfile.name())) {
            settingsPath = "thermostat." + smartHomeProfile.name();
            config = this.getLoadedConfig();
            STEMSystemApp.LOGGER.CONFIG("Load custom thermostat settings CUSTOM:" + smartHomeProfile.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        } else if (getDefaultConfig().contains("thermostat." + smartHomeProfile.name())) {
            settingsPath = "thermostat." + smartHomeProfile.name();
            config = getDefaultConfig();
            STEMSystemApp.LOGGER.CONFIG("Load default thermostat settings DEFAULT:" + smartHomeProfile.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        } else if (this.getLoadedConfig().contains("thermostat." + SmartHomeProfile.DEFAULT.name())) {
            settingsPath = "thermostat." + SmartHomeProfile.DEFAULT.name();
            config = getLoadedConfig();
            STEMSystemApp.LOGGER.WARNING("Load custom default thermostat settings CUSTOM:" + SmartHomeProfile.DEFAULT.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        } else {
            config = getDefaultConfig();
            settingsPath = "thermostat." + SmartHomeProfile.DEFAULT.name();
            STEMSystemApp.LOGGER.WARNING("Load default fallback thermostat settings DEFAULT:" + SmartHomeProfile.DEFAULT.name() + " for hardId " + this.getDeviceHardAddress() + " configName " + this.getName());
        }

        Map<String, Map> objectMap = (LinkedHashMap<String, Map>) config.get(settingsPath);

        for (String key : objectMap.keySet()) {
            LocalTime localTime = LocalTime.parse(config.getString(settingsPath + "." + key + ".time"), dateTimeFormatter);
            double value = config.getDouble(settingsPath + "." + key + ".value");
            STEMSystemApp.LOGGER.CONFIG("Add thermostat timer for hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " time: " + localTime.toString() + " value: " + value);
            this.thermostatTimerList.add(new Pair<>(localTime, value));
        }

        int moveCounter = 0;
        for (int i = 0; i < this.thermostatTimerList.size(); i++) {
            Pair<LocalTime, Double> first = this.thermostatTimerList.getFirst();
            LocalTime offSetTime = first.getKey().plus(thermostatOffSet, ChronoUnit.MILLIS);
            if (offSetTime.isBefore(LocalTime.now())) {
                first = this.thermostatTimerList.removeFirst();
                this.thermostatTimerList.addLast(first);
                STEMSystemApp.LOGGER.CONFIG("Add thermostat timer to tail in timer list:  " + first.getKey().toString() + " value: " + first.getValue());
                moveCounter++;
                continue;
            }
            break;
        }
        STEMSystemApp.LOGGER.CONFIG("Resort timer list done. Remaining timers for today: " + (this.thermostatTimerList.size() - moveCounter));
    }

    private void run() {
        ZigbeeThermostatDevice zigbeeThermostatDevice = (ZigbeeThermostatDevice) this.mqttDevice;
        while (true) {
            try {
                if (!this.thermostatTimerList.isEmpty()) {
                    Pair<LocalTime, Double> first = this.thermostatTimerList.getFirst();
                    LocalTime offSetTime = first.getKey().plus(this.thermostatOffSet, ChronoUnit.MILLIS);

                    if (first.getKey().isBefore(LocalTime.now()) && offSetTime.isAfter(LocalTime.now())) {
                        first = this.thermostatTimerList.removeFirst();
                        STEMSystemApp.LOGGER.INFO("Set thermostat " + this.getName() + " value to:  " + first.getKey().toString() + " value: " + first.getValue());
                        zigbeeThermostatDevice.setTemperature(first.getValue());
                        this.thermostatTimerList.addLast(first);
                    }
                }
                Thread.sleep(50);
            } catch (Exception e) {
                STEMSystemApp.LOGGER.ERROR(e);
            }
        }
    }

}