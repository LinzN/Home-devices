package de.linzn.homeDevices.profiles;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.events.AutoStartStopTimerEvent;
import de.linzn.homeDevices.events.AutoSwitchOffTimerEvent;
import de.linzn.openJL.pairs.Pair;
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
    final MqttSwitch mqttSwitch;
    private final long autoStartStopTimerOffSet = 200;
    private boolean autoSwitchOffEnabled;
    private long autoSwitchOffTimer;
    private LocalTime autoSwitchOffStartTime;
    private LocalTime autoSwitchOffStopTime;
    private boolean autoStartStopEnabled;
    private LinkedList<Pair<LocalTime, Boolean>> autoStartStopTimerList;


    public SwitchDeviceProfile(MqttDevice mqttDevice) {
        super(mqttDevice);
        this.mqttSwitch = (MqttSwitch) mqttDevice;
    }

    @Override
    public void loadProfile() {
        this.loadAutoSwitchOffTimer();
        this.loadAutoStartStopTimer();
    }

    @Override
    public void runProfile() {
        STEMSystemApp.getInstance().getScheduler().runRepeatScheduler(HomeDevicesPlugin.homeDevicesPlugin, this::runAutoSwitchOffTimer, 10, 3, TimeUnit.SECONDS);
        STEMSystemApp.getInstance().getScheduler().runTaskLater(HomeDevicesPlugin.homeDevicesPlugin, this::runAutoStartStopTimer, 2, TimeUnit.SECONDS);
    }

    /* AutoSwitchOffTimer */

    private void loadAutoSwitchOffTimer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm");

        String optionPath = "autoModeSwitchOffSettings";

        if (this.hasOwnConfig() && this.getLoadedConfig().contains(optionPath)) {
            this.autoSwitchOffEnabled = this.getLoadedConfig().getBoolean(optionPath + ".autoSwitchOffEnabled");
            this.autoSwitchOffTimer = this.getLoadedConfig().getInt(optionPath + ".autoSwitchOffAfterSeconds") * 1000L;
            this.autoSwitchOffStartTime = LocalTime.parse(this.getLoadedConfig().getString(optionPath + ".startTime"), dateTimeFormatter);
            this.autoSwitchOffStopTime = LocalTime.parse(this.getLoadedConfig().getString(optionPath + ".stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.CONFIG("Load specific autoSwitchOff settings for hardId " + mqttSwitch.getDeviceHardAddress() + " configName " + mqttSwitch.getConfigName());
        } else {
            this.autoSwitchOffEnabled = this.getDefaultConfig().getBoolean("category." + mqttSwitch.getSwitchCategory().name() + ".autoSwitchOffEnabled");
            this.autoSwitchOffTimer = this.getDefaultConfig().getInt("category." + mqttSwitch.getSwitchCategory().name() + ".autoSwitchOffAfterSeconds") * 1000L;
            this.autoSwitchOffStartTime = LocalTime.parse(this.getDefaultConfig().getString("category." + mqttSwitch.getSwitchCategory().name() + ".startTime"), dateTimeFormatter);
            this.autoSwitchOffStopTime = LocalTime.parse(this.getDefaultConfig().getString("category." + mqttSwitch.getSwitchCategory().name() + ".stopTime"), dateTimeFormatter);
            STEMSystemApp.LOGGER.CONFIG("No autoSwitchOff settings found for hardId " + mqttSwitch.getDeviceHardAddress() + " configName  " + mqttSwitch.getConfigName());
            STEMSystemApp.LOGGER.CONFIG("Load default settings from category " + mqttSwitch.getSwitchCategory().name());
        }

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

    private void runAutoSwitchOffTimer() {
        if (HomeDevicesPlugin.homeDevicesPlugin.isCategoryInAutoSwitchOffMode(this.mqttSwitch.getSwitchCategory())) {
            if (this.mqttSwitch.deviceStatus != null && this.mqttSwitch.deviceStatus.get()) {
                if (this.canBeAutoSwitchOff(this.mqttSwitch.lastSwitch.getTime())) {
                    AutoSwitchOffTimerEvent autoSwitchOffTimerEvent = new AutoSwitchOffTimerEvent(this.mqttSwitch, autoSwitchOffStartTime, autoSwitchOffStopTime, this.mqttSwitch.lastSwitch);
                    STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(autoSwitchOffTimerEvent);
                    if (!autoSwitchOffTimerEvent.isCanceled()) {
                        STEMSystemApp.LOGGER.INFO("Auto-switch off hardId: " + this.mqttSwitch.deviceHardAddress + " configName: " + this.mqttSwitch.configName + " after: " + ((int) (autoSwitchOffTimer / 1000)) + " seconds!");
                        this.mqttSwitch.switchDevice(false);
                    }
                }
            }
        }
    }


    private void loadAutoStartStopTimer() {
        this.autoStartStopTimerList = new LinkedList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss.SSS");

        String optionPath = "autoStartStopTimer";

        if (this.hasOwnConfig() && this.getLoadedConfig().contains(optionPath)) {
            autoStartStopEnabled = true;
            Map<String, Map> objectMap = (LinkedHashMap<String, Map>) this.getLoadedConfig().get(optionPath);

            for (String key : objectMap.keySet()) {
                LocalTime localTime = LocalTime.parse(this.getLoadedConfig().getString(optionPath + "." + key + ".time"), dateTimeFormatter);
                boolean value = this.getLoadedConfig().getBoolean(optionPath + "." + key + ".value");
                STEMSystemApp.LOGGER.CONFIG("Add timer for hardId: " + this.mqttSwitch.deviceHardAddress + " configName: " + this.mqttSwitch.configName + " time: " + localTime.toString() + " value: " + value);
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
                            STEMSystemApp.LOGGER.INFO("Timer switch hardId: " + this.mqttSwitch.deviceHardAddress + " configName: " + this.mqttSwitch.configName + " status: " + first.getValue());
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



