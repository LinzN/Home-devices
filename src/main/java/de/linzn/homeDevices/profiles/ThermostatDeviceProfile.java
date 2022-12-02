package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
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

    private void loadThermostatTimer() {
        this.thermostatTimerList = new LinkedList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

        String optionPath = "thermostat";

        if (this.getLoadedConfig().contains(optionPath)) {
            Map<String, Map> objectMap = (LinkedHashMap<String, Map>) this.getLoadedConfig().get(optionPath);

            for (String key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(this.getLoadedConfig().getString(optionPath + "." + key + ".time"), dateTimeFormatter);
                double value = this.getLoadedConfig().getDouble(optionPath + "." + key + ".value");
                STEMSystemApp.LOGGER.CONFIG("Add thermostat timer for hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " time: " + localTime.toString() + " value: " + value);
                this.thermostatTimerList.add(new Pair<>(localTime, value));
            }

        } else {
            Map<String, Map> objectMap = (LinkedHashMap<String, Map>) DeviceProfile.getDefaultConfig().get(optionPath);

            for (String key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(DeviceProfile.getDefaultConfig().getString(optionPath + "." + key + ".time"), dateTimeFormatter);
                double value = DeviceProfile.getDefaultConfig().getDouble(optionPath + "." + key + ".value");
                STEMSystemApp.LOGGER.CONFIG("Add default thermostat timer for hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " time: " + localTime.toString() + " value: " + value);
                this.thermostatTimerList.add(new Pair<>(localTime, value));
            }
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
                        STEMSystemApp.LOGGER.INFO("Set thermostat value to:  " + first.getKey().toString() + " value: " + first.getValue());
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