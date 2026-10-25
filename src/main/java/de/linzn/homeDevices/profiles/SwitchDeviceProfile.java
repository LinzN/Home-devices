package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.devices.enums.DeviceTechnology;
import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.cancelable.AutoStartStopTimerEvent;
import de.linzn.homeDevices.events.cancelable.AutoSwitchOffTimerEvent;
import de.linzn.openJL.pairs.Pair;
import de.linzn.simplyConfiguration.FileConfiguration;
import de.linzn.stem.api.scheduler.ScheduledTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SwitchDeviceProfile extends DeviceProfile {

    private static final DateTimeFormatter SWITCH_OFF_FORMAT = DateTimeFormatter.ofPattern("HH-mm");
    private static final DateTimeFormatter TIMER_FORMAT = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

    private final DailyTimer<Boolean> autoStartStopTimer;
    private volatile MqttSwitch mqttSwitch;
    /* replaced as a whole on profile change, read by the scheduler thread */
    private volatile AutoSwitchOffSettings autoSwitchOffSettings = AutoSwitchOffSettings.DISABLED;
    private ScheduledTask autoSwitchOffTask;

    public SwitchDeviceProfile(FileConfiguration profileConfig, String name, String deviceHardAddress, String description, DeviceTechnology deviceTechnology, MqttDeviceCategory mqttDeviceCategory, String subDeviceCategory) {
        super(profileConfig, name, deviceHardAddress, description, deviceTechnology, mqttDeviceCategory, subDeviceCategory);
        this.autoStartStopTimer = new DailyTimer<>(this.getPlugin(), "switch " + name, this::runAutoStartStopTimer);
    }

    @Override
    public void loadProfile() {
        this.mqttSwitch = (MqttSwitch) mqttDevice;
        this.loadAutoSwitchOffTimer();
        this.loadAutoStartStopTimer();
    }

    @Override
    public synchronized void runProfile() {
        if (this.autoSwitchOffTask == null) {
            this.autoSwitchOffTask = this.getPlugin().getStemApi().scheduler().runRepeating(this::runAutoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        }
        this.autoStartStopTimer.start();
    }

    @Override
    public synchronized void stopProfile() {
        if (this.autoSwitchOffTask != null) {
            this.autoSwitchOffTask.cancel();
            this.autoSwitchOffTask = null;
        }
        this.autoStartStopTimer.stop();
    }

    @Override
    public boolean changeSmartProfile() {
        try {
            this.loadAutoSwitchOffTimer();
            this.loadAutoStartStopTimer();
            return true;
        } catch (Exception e) {
            this.getPlugin().getPluginLogger().error("Invalid switch settings for " + this.getName(), e);
            return false;
        }
    }

    /* AutoSwitchOffTimer */
    private void loadAutoSwitchOffTimer() {
        SmartHomeProfile smartHomeProfile = this.getPlugin().getProfileController().getCurrentProfile();
        String category = mqttSwitch.getSwitchCategory().name();
        String settingsPath;
        FileConfiguration config;
        if (this.getLoadedConfig().contains("autoModeSwitchOffSettings." + smartHomeProfile.name())) {
            config = this.getLoadedConfig();
            settingsPath = "autoModeSwitchOffSettings." + smartHomeProfile.name();
            this.getPlugin().getPluginLogger().debug("Load custom autoSwitchOff settings " + smartHomeProfile.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else if (getDefaultConfig().contains("category." + category + "." + smartHomeProfile.name())) {
            config = getDefaultConfig();
            settingsPath = "category." + category + "." + smartHomeProfile.name();
            this.getPlugin().getPluginLogger().debug("Load default autoSwitchOff settings " + category + ":" + smartHomeProfile.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else if (this.getLoadedConfig().contains("autoModeSwitchOffSettings." + SmartHomeProfile.DEFAULT.name())) {
            config = this.getLoadedConfig();
            settingsPath = "autoModeSwitchOffSettings." + SmartHomeProfile.DEFAULT.name();
            this.getPlugin().getPluginLogger().warning("Load custom default autoSwitchOff settings " + SmartHomeProfile.DEFAULT.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else {
            config = getDefaultConfig();
            settingsPath = "category." + category + "." + SmartHomeProfile.DEFAULT.name();
            this.getPlugin().getPluginLogger().warning("Load default fallback autoSwitchOff settings " + category + ":" + SmartHomeProfile.DEFAULT.name() + " for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        }
        if (!config.contains(settingsPath)) {
            this.getPlugin().getPluginLogger().warning("No autoSwitchOff settings found for " + this.getName() + ", auto switch off disabled");
            this.autoSwitchOffSettings = AutoSwitchOffSettings.DISABLED;
            return;
        }

        boolean enabled = config.getBoolean(settingsPath + ".autoSwitchOffEnabled");
        long timer = config.getInt(settingsPath + ".autoSwitchOffAfterSeconds") * 1000L;
        LocalTime startTime = LocalTime.parse(config.getString(settingsPath + ".startTime"), SWITCH_OFF_FORMAT);
        LocalTime stopTime = LocalTime.parse(config.getString(settingsPath + ".stopTime"), SWITCH_OFF_FORMAT);
        if (enabled && startTime.equals(stopTime)) {
            this.getPlugin().getPluginLogger().error("Start and stop are the same LocalTime for " + this.getName() + "! This is useless!");
        }
        this.autoSwitchOffSettings = new AutoSwitchOffSettings(enabled, timer, startTime, stopTime);
    }

    private boolean isCategoryInAutoSwitchOffMode() {
        SmartHomeProfile smartHomeProfile = this.getPlugin().getProfileController().getCurrentProfile();
        String category = mqttSwitch.getSwitchCategory().name();
        String settingsPath;
        if (getDefaultConfig().contains("category." + category + "." + smartHomeProfile.name())) {
            settingsPath = "category." + category + "." + smartHomeProfile.name();
        } else {
            settingsPath = "category." + category + "." + SmartHomeProfile.DEFAULT.name();
        }
        return getDefaultConfig().getBoolean(settingsPath + ".autoSwitchOffEnabled");
    }

    private void runAutoSwitchOffTimer() {
        MqttSwitch device = this.mqttSwitch;
        if (device == null || !device.isSwitchedOn() || !isCategoryInAutoSwitchOffMode()) {
            return;
        }
        AutoSwitchOffSettings settings = this.autoSwitchOffSettings;
        Date lastSwitch = device.getLastSwitch();
        if (lastSwitch == null || !settings.canBeSwitchedOff(lastSwitch.getTime())) {
            return;
        }
        AutoSwitchOffTimerEvent autoSwitchOffTimerEvent = new AutoSwitchOffTimerEvent(device, settings.startTime(), settings.stopTime(), lastSwitch);
        this.getPlugin().getStemApi().events().fire(autoSwitchOffTimerEvent);
        if (!autoSwitchOffTimerEvent.isCanceled()) {
            this.getPlugin().getPluginLogger().info("Auto-switch off hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " after: " + (settings.timerMillis() / 1000) + " seconds!");
            device.switchDevice(false);
        }
    }

    /* AutoStartStopTimer */
    private void loadAutoStartStopTimer() {
        SmartHomeProfile smartHomeProfile = this.getPlugin().getProfileController().getCurrentProfile();
        String optionPath = "autoStartStopTimer." + smartHomeProfile.name();
        List<Map.Entry<LocalTime, Boolean>> entries = new ArrayList<>();
        if (this.getLoadedConfig().get(optionPath) instanceof Map<?, ?> objectMap) {
            for (Object key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(this.getLoadedConfig().getString(optionPath + "." + key + ".time"), TIMER_FORMAT);
                boolean value = this.getLoadedConfig().getBoolean(optionPath + "." + key + ".value");
                this.getPlugin().getPluginLogger().debug("Add timer for " + smartHomeProfile.name() + " hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " time: " + localTime + " value: " + value);
                entries.add(Map.entry(localTime, value));
            }
        }
        this.autoStartStopTimer.setEntries(entries);
    }

    private void runAutoStartStopTimer(LocalTime time, Boolean value) {
        MqttSwitch device = this.mqttSwitch;
        if (device == null || !device.hasData()) {
            return;
        }
        AutoStartStopTimerEvent autoStartStopTimerEvent = new AutoStartStopTimerEvent(device, new Pair<>(time, value));
        this.getPlugin().getStemApi().events().fire(autoStartStopTimerEvent);
        if (!autoStartStopTimerEvent.isCanceled()) {
            this.getPlugin().getPluginLogger().info("Timer switch hardId: " + this.getDeviceHardAddress() + " configName: " + this.getName() + " status: " + value);
            device.switchDevice(value);
        }
    }

    private record AutoSwitchOffSettings(boolean enabled, long timerMillis, LocalTime startTime, LocalTime stopTime) {

        static final AutoSwitchOffSettings DISABLED = new AutoSwitchOffSettings(false, 0, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);

        boolean canBeSwitchedOff(long lastSwitch) {
            return this.enabled && this.isInRange(LocalTime.now()) && (lastSwitch + this.timerMillis < System.currentTimeMillis());
        }

        private boolean isInRange(LocalTime now) {
            if (this.stopTime.isBefore(this.startTime)) {
                return this.startTime.isBefore(now) || this.stopTime.isAfter(now);
            }
            return this.startTime.isBefore(now) && this.stopTime.isAfter(now);
        }
    }
}
