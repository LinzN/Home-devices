package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.cancelable.AutoStartStopTimerEvent;
import de.linzn.homeDevices.events.cancelable.AutoSwitchOffTimerEvent;
import de.linzn.openJL.pairs.Pair;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.stem.stemSystem.STEMSystemApp;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SwitchDeviceProfile extends DeviceProfile {
    private final long autoStartStopTimerOffSet = 200;
    MqttSwitch mqttSwitch;
    private boolean autoSwitchOffEnabled;
    private long autoSwitchOffTimer;
    private LocalTime autoSwitchOffStartTime;
    private LocalTime autoSwitchOffStopTime;
    private boolean autoStartStopEnabled;
    private LinkedList<Pair<LocalTime, Boolean>> autoStartStopTimerList;


    public SwitchDeviceProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);

    }

    @Override
    public void loadProfile() {
        this.mqttSwitch = (MqttSwitch) mqttDevice;
        this.loadAutoSwitchOffTimer();
        this.loadAutoStartStopTimer();
    }

    @Override
    public void runProfile() {
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, this::runAutoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, this::runAutoStartStopTimer, 2, TimeUnit.SECONDS);
    }

    @Override
    public boolean changeSmartProfile() {
        this.loadAutoSwitchOffTimer();
        this.loadAutoStartStopTimer();
        return true;
    }

    /* AutoSwitchOffTimer */

    private void loadAutoSwitchOffTimer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm");
        SmartHomeProfile smartHomeProfile = HomeDevicesPlugin.homeDevicesPlugin.getProfileController().getCurrentProfile();

        String settingsPath;
        FileConfiguration config;

        if (this.getLoadedConfig().contains("autoModeSwitchOffSettings." + smartHomeProfile.name())) {
            config = this.getLoadedConfig();
            settingsPath = "autoModeSwitchOffSettings." + smartHomeProfile.name();
            STEMSystemApp.LOGGER.CONFIG("Load custom autoSwitchOff settings " + smartHomeProfile.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else if (getDefaultConfig().contains("category." + mqttSwitch.getSwitchCategory().name() + "." + smartHomeProfile.name())) {
            config = getDefaultConfig();
            settingsPath = "category." + mqttSwitch.getSwitchCategory().name() + "." + smartHomeProfile.name();
            STEMSystemApp.LOGGER.CONFIG("Load default autoSwitchOff settings " + mqttSwitch.getSwitchCategory().name() + ":" + smartHomeProfile.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else if (this.getLoadedConfig().contains("autoModeSwitchOffSettings." + SmartHomeProfile.DEFAULT.name())) {
            config = this.getLoadedConfig();
            settingsPath = "autoModeSwitchOffSettings." + SmartHomeProfile.DEFAULT.name();
            STEMSystemApp.LOGGER.WARNING("Load custom default autoSwitchOff settings " + SmartHomeProfile.DEFAULT.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else {
            config = getDefaultConfig();
            settingsPath = "category." + mqttSwitch.getSwitchCategory().name() + "." + SmartHomeProfile.DEFAULT.name();
            STEMSystemApp.LOGGER.WARNING("Load default fallback autoSwitchOff settings " + mqttSwitch.getSwitchCategory().name() + ":" + SmartHomeProfile.DEFAULT.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        }

        this.autoSwitchOffEnabled = config.getBoolean(settingsPath + ".autoSwitchOffEnabled");
        this.autoSwitchOffTimer = config.getInt(settingsPath + ".autoSwitchOffAfterSeconds") * 1000L;
        this.autoSwitchOffStartTime = LocalTime.parse(config.getString(settingsPath + ".startTime"), dateTimeFormatter);
        this.autoSwitchOffStopTime = LocalTime.parse(config.getString(settingsPath + ".stopTime"), dateTimeFormatter);

        if (this.autoSwitchOffEnabled && this.autoSwitchOffStartTime.equals(this.autoSwitchOffStopTime)) {
            STEMSystemApp.LOGGER.ERROR("Start and stop are the same LocalTime! This is useless!");
        }
    }


    private boolean canBeAutoSwitchOff(long lastSwitch) {
        if (this.autoSwitchOffEnabled) {
            return isInAutoSwitchOffTimerRange() && (lastSwitch + autoSwitchOffTimer < new Date().getTime());
        } else {
            return false;
        }
    }

    private boolean isInAutoSwitchOffTimerRange() {
        if (this.autoSwitchOffStopTime.isBefore(this.autoSwitchOffStartTime)) {
            return this.autoSwitchOffStartTime.isBefore(LocalTime.now()) || this.autoSwitchOffStopTime.isAfter(LocalTime.now());
        } else {
            return this.autoSwitchOffStartTime.isBefore(LocalTime.now()) && this.autoSwitchOffStopTime.isAfter(LocalTime.now());
        }
    }

    private boolean isCategoryInAutoSwitchOffMode() {
        String settingsPath;
        FileConfiguration config;

        SmartHomeProfile smartHomeProfile = HomeDevicesPlugin.homeDevicesPlugin.getProfileController().getCurrentProfile();
        if (getDefaultConfig().contains("category." + mqttSwitch.getSwitchCategory().name() + "." + smartHomeProfile.name())) {
            config = getDefaultConfig();
            settingsPath = "category." + mqttSwitch.getSwitchCategory().name() + "." + smartHomeProfile.name();
        } else {
            config = getDefaultConfig();
            settingsPath = "category." + mqttSwitch.getSwitchCategory().name() + "." + SmartHomeProfile.DEFAULT.name();
        }

        return config.getBoolean(settingsPath + ".autoSwitchOffEnabled");
    }

    private void runAutoSwitchOffTimer() {
        if (isCategoryInAutoSwitchOffMode()) {
            if (this.mqttSwitch.deviceStatus != null && this.mqttSwitch.deviceStatus.get()) {
                if (this.canBeAutoSwitchOff(this.mqttSwitch.lastSwitch.getTime())) {
                    AutoSwitchOffTimerEvent autoSwitchOffTimerEvent = new AutoSwitchOffTimerEvent(this.mqttSwitch, autoSwitchOffStartTime, autoSwitchOffStopTime, this.mqttSwitch.lastSwitch);
                    STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(autoSwitchOffTimerEvent);
                    if (!autoSwitchOffTimerEvent.isCanceled()) {
                        STEMSystemApp.LOGGER.INFO("Auto-switch off hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " after: " + ((int) (autoSwitchOffTimer / 1000)) + " seconds!");
                        this.mqttSwitch.switchDevice(false);
                    }
                }
            }
        }
    }


    private void loadAutoStartStopTimer() {
        this.autoStartStopTimerList = new LinkedList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

        SmartHomeProfile smartHomeProfile = HomeDevicesPlugin.homeDevicesPlugin.getProfileController().getCurrentProfile();

        String optionPath = "autoStartStopTimer." + smartHomeProfile.name();

        if (this.getLoadedConfig().contains(optionPath)) {
            autoStartStopEnabled = true;
            Map<String, Map> objectMap = (LinkedHashMap<String, Map>) this.getLoadedConfig().get(optionPath);

            for (String key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(this.getLoadedConfig().getString(optionPath + "." + key + ".time"), dateTimeFormatter);
                boolean value = this.getLoadedConfig().getBoolean(optionPath + "." + key + ".value");
                STEMSystemApp.LOGGER.CONFIG("Add timer for " + smartHomeProfile.name() + " hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " time: " + localTime.toString() + " value: " + value);
                this.autoStartStopTimerList.add(new Pair<>(localTime, value));
            }

        } else {
            autoStartStopEnabled = false;
        }

        if (autoStartStopEnabled) {
            int moveCounter = 0;
            for (int i = 0; i < this.autoStartStopTimerList.size(); i++) {
                Pair<LocalTime, Boolean> first = this.autoStartStopTimerList.getFirst();
                LocalTime offSetTime = first.getKey().plus(autoStartStopTimerOffSet, ChronoUnit.MILLIS);
                if (offSetTime.isBefore(LocalTime.now())) {
                    first = this.autoStartStopTimerList.removeFirst();
                    this.autoStartStopTimerList.addLast(first);
                    STEMSystemApp.LOGGER.CONFIG("Add timer to tail in timer list:  " + first.getKey().toString() + " value: " + first.getValue());
                    moveCounter++;
                    continue;
                }
                break;
            }
            STEMSystemApp.LOGGER.CONFIG("Resort timer list done. Remaining timers for today: " + (this.autoStartStopTimerList.size() - moveCounter));
        }
    }

    private void runAutoStartStopTimer() {
        while (true) {
            try {
                if (this.autoStartStopEnabled && this.mqttSwitch.deviceStatus != null) {
                    Pair<LocalTime, Boolean> first = this.autoStartStopTimerList.getFirst();
                    LocalTime offSetTime = first.getKey().plus(this.autoStartStopTimerOffSet, ChronoUnit.MILLIS);
                    if (first.getKey().isBefore(LocalTime.now()) && offSetTime.isAfter(LocalTime.now())) {
                        AutoStartStopTimerEvent autoStartStopTimerEvent = new AutoStartStopTimerEvent(this.mqttSwitch, first);
                        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(autoStartStopTimerEvent);
                        first = this.autoStartStopTimerList.removeFirst();
                        if (!autoStartStopTimerEvent.isCanceled()) {
                            /* switch device */
                            STEMSystemApp.LOGGER.INFO("Timer switch hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " status: " + first.getValue());
                            this.mqttSwitch.switchDevice(first.getValue());
                            /* switch device */
                        }
                        this.autoStartStopTimerList.addLast(first);
                    }
                }
                Thread.sleep(50);
            } catch (Exception e) {
                STEMSystemApp.LOGGER.ERROR(e);
            }
        }
    }

}



