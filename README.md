![MirraNET Logo](https://raw.githubusercontent.com/LinzN/Stem/master/stem-core/src/main/resources/header-logo.png)

# home-devices

> Smart home devices for [STEM](https://github.com/LinzN/Stem): switches, sensors, thermostats, printers and more via MQTT.

home-devices connects STEM to the devices in the house. Every device is described by a profile file and
talks to STEM over MQTT (Tasmota, Zigbee via a Tasmota gateway, nanoCLR boards and MQTT bridges for
3D printers and air conditioners). Smart home profiles switch the behaviour of all devices at once, for
example automatic switch-off and time switches.

---

## Features

- Device types: Tasmota and Zigbee switches, environment and door sensors, Zigbee thermostats, power
  meters, UPS, RF433 garage module, LED strips, nanoCLR door bell and keyboard, AnkerMake, Snapmaker and
  Midea devices
- **Smart home profiles** `DEFAULT`, `AUTOMATIC`, `HOMEOFFICE`, `HOLYDAY`, `STANDBY`
- **Automatic switch-off**: switches turn off after a configured time within a time window
- **Time switches**: switches on/off at fixed times (millisecond precision), per profile
- **Thermostat timers**: target temperatures at fixed times, per profile
- Notifications and display information for open doors, door bell, UPS on battery and finished 3D prints
- Web API, StemLink bridge and health check for all devices
- One broken profile file does not stop the other devices from loading

---

## Requirements

| Requirement | Version                                                                 |
|-------------|-------------------------------------------------------------------------|
| STEM        | 1.2.0 or newer (full `stem` dependency, uses StemLink), MQTT module configured |
| Plugins     | [web-api](https://github.com/LinzN/web-api)                             |

---

## Installation

1. Build the plugin with `mvn clean package` (or take the build from Jenkins).
2. Copy `target/home-devices.jar` into the `plugins` folder of STEM.
3. Create `plugins/home-devices/profiles/default.yml` and one profile file per device (see below).
4. Start STEM (plugins are only loaded at startup).

For Hama TS0601 thermostats, load the Tasmota Zigbee converter
[`Hama_TS0601.zb`](src/main/resources/Hama_TS0601.zb) on the Zigbee gateway.

---

## Configuration

All files are in `plugins/home-devices/profiles/`. Every file except `default.yml` describes one device.

### Device profile

```yaml
deviceInfo:
  name: livingroom-light          # unique name, used by the web API and other plugins
  hardwareAddress: "0x1A2B"       # Tasmota topic, Zigbee short address or bridge device name
  description: Ceiling light living room
  deviceTechnology: ZIGBEE        # TASMOTA, ZIGBEE, WINDOWS, NANOCLR, OTHER
  deviceCategory: SWITCH          # see table below
  subDeviceCategory: LIGHT        # switches: LIGHT/OTHER, sensors: ENVIRONMENT/DOOR
options:
  zigbeeGateway: zigbee-gw        # only Zigbee; falls back to default.yml
```

| `deviceCategory`    | Technology        | Subscribed topics                                                         | Published topics                         |
|---------------------|-------------------|---------------------------------------------------------------------------|------------------------------------------|
| `SWITCH`            | `TASMOTA`         | `stat/<address>/RESULT`                                                   | `cmnd/<address>/Power`                   |
| `SWITCH`            | `ZIGBEE`          | `tele/<gateway>/<address>/SENSOR`                                          | `cmnd/<gateway>/<address>/zbsend`, `.../zblight` |
| `SENSOR` (`ENVIRONMENT`) | `ZIGBEE`     | `tele/<gateway>/<address>/SENSOR`                                          | `cmnd/<gateway>/zbinfo`                  |
| `SENSOR` (`DOOR`)   | `ZIGBEE`          | `tele/<gateway>/<address>/SENSOR`                                          | –                                        |
| `THERMOSTAT`        | `ZIGBEE`          | `tele/<gateway>/<address>/SENSOR`                                          | `cmnd/<gateway>/<address>/zbsend`        |
| `POWERCONSUMPTION`  | any               | `tele/<address>/SENSOR`                                                    | –                                        |
| `USV`               | any               | `stat/<address>/status`                                                    | –                                        |
| `RF433`             | any               | `stat/<address>/data`                                                      | `custom.rf433MQTT`                       |
| `LED`               | any               | `stat/<address>/data`                                                      | `cmnd/<address>/data`                    |
| `DOORRING`          | any               | `stat/nanoCLR/<address>/data`                                              | `custom.rf433MQTT`                       |
| `NANOCLR_KEYBOARD`  | any               | `stat/nanoCLR/<address>/data`                                              | `cmnd/nanoCLR/<address>/data`            |
| `ANKERMAKE_PRINTER` | any               | `ankermake2mqtt/printers/<address>/data`                                   | –                                        |
| `SNAPMAKER_PRINTER` | any               | `snapmaker2mqtt/<address>/state`, `/availability`, `/event/print_*`, `snapmaker2mqtt/bridge/status` | – (read only) |
| `MIDEA_AC`          | any               | `midea2mqtt/<address>/state`, `/capabilities`                              | `midea2mqtt/<address>/get`, `/set`       |

Optional sections of a device profile:

```yaml
# Switches: own automatic switch-off per smart home profile (otherwise default.yml is used)
autoModeSwitchOffSettings:
  DEFAULT:
    autoSwitchOffEnabled: true
    autoSwitchOffAfterSeconds: 900
    startTime: "22-00"            # HH-mm
    stopTime: "06-00"

# Switches: time switches per smart home profile
autoStartStopTimer:
  DEFAULT:
    morning:
      time: "06-30-00.000"        # HH-mm-ss.SSS
      value: true
    night:
      time: "23-00-00.000"
      value: false

# Thermostats: target temperatures per smart home profile (otherwise default.yml is used)
thermostat:
  DEFAULT:
    day:
      time: "06-00-00.000"
      value: 21.0
    night:
      time: "22-00-00.000"
      value: 17.0

# Environment sensors: correction values
offset:
  temperature: -0.5
  humidity: 2.0

# RF433 garage module and door bell
custom:
  rf433MQTT: "cmnd/rf433/send"    # topic the command is published to
  codeWord: "123456"              # door bell only: code sent on ring
```

### `default.yml`

```yaml
options:
  zigbeeGateway: zigbee-gw
category:                         # automatic switch-off per switch category and smart home profile
  LIGHT:
    DEFAULT:
      autoSwitchOffEnabled: true
      autoSwitchOffAfterSeconds: 1800
      startTime: "00-00"
      stopTime: "06-00"
  OTHER:
    DEFAULT:
      autoSwitchOffEnabled: false
      autoSwitchOffAfterSeconds: 0
      startTime: "00-00"
      stopTime: "00-00"
thermostat:                       # default thermostat timers per smart home profile
  DEFAULT:
    day:
      time: "06-00-00.000"
      value: 20.0
```

Settings are looked up in this order: device file for the current profile, `default.yml` for the
current profile, device file for `DEFAULT`, `default.yml` for `DEFAULT`. Automatic switch-off only runs
if it is enabled for the switch category in `default.yml`. All values above are examples.

---

## Web API

All requests are `POST` with a JSON body.

| Path                           | Body                                                                         | Description                                 |
|--------------------------------|------------------------------------------------------------------------------|---------------------------------------------|
| `/homedevice/device`           | `{"deviceName": "...", "requestAction": "READ"}`                             | Current data of a device                    |
| `/homedevice/device`           | `{"deviceName": "...", "requestAction": "WRITE", ...}`                      | Change a device (see below)                 |
| `/homedevice/status`           | –                                                                            | Current profile and switches per category   |
| `/homedevice/smarthomeprofile` | `{"requestAction": "READ", "current": true}` or `"available": true`          | Current or available profiles               |
| `/homedevice/smarthomeprofile` | `{"requestAction": "WRITE", "profile": "STANDBY"}`                           | Change the smart home profile               |

`WRITE` fields per device type:

| Device       | Fields                                                   |
|--------------|----------------------------------------------------------|
| Switch       | `status` (true/false), `brightness` (0–255, Zigbee only) |
| Thermostat   | `current_heating_setpoint` (°C)                          |
| LED          | `mode`, `r`, `g`, `b` (0–255)                            |
| RF433        | `trigger_garage_event`                                   |
| Midea AC     | `commands` (JSON object, forwarded to midea2mqtt)        |

---

## StemLink

| Channel             | Direction | Content                                                            |
|---------------------|-----------|--------------------------------------------------------------------|
| `switchable_device` | in        | `configName`, action `switch_status` (+ boolean) or `toggle_status` |
| `tasmota_device`    | out       | `configName`, `update_status`, boolean — sent on every switch change |

---

## Events

| Event                              | Cancelable | Fired when                                     |
|------------------------------------|------------|------------------------------------------------|
| `DeviceUpdateEvent`                | no         | a switch reported a new status                 |
| `MQTTUpdateDeviceEvent`            | no         | a Tasmota switch sent a power result           |
| `SwitchDeviceEvent`                | yes        | a switch is about to be switched               |
| `ToggleDeviceDeviceEvent`          | yes        | a switch is about to be toggled                |
| `AutoSwitchOffTimerEvent`          | yes        | the automatic switch-off is about to run       |
| `AutoStartStopTimerEvent`          | yes        | a time switch is about to run                  |
| `EnvironmentSensorUpdateDataEvent` | no         | an environment sensor sent data                |
| `PowerConsumptionUpdateDataEvent`  | no         | a power meter sent data                        |
| `MQTTDoorRingEvent`                | no         | the door bell was pressed                      |
| `MQTTPrintEndEvent`                | no         | an AnkerMake print finished                    |
| `MQTTSnapmakerPrintEndEvent`       | no         | a Snapmaker print finished, failed or was canceled |

---

## Health check

Every health check asks all devices for their status, waits two minutes and reports every device that
did not answer as `ERROR`.

---

## API for other plugins

```java
DeviceManager devices = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager();
MqttDevice device = devices.getMqttDevice("livingroom-light");   // null if unknown
if (device instanceof MqttSwitch mqttSwitch && mqttSwitch.hasData()) {
    mqttSwitch.switchDevice(!mqttSwitch.isSwitchedOn());
}

HomeDevicesPlugin.homeDevicesPlugin.getProfileController().requestProfileChange(SmartHomeProfile.STANDBY);
```

Add `home-devices` as `provided` dependency and to `depend` in `plugin.yml`.

---

## License

Copyright (c) MirraNET, Niklas Linz. Licensed under the
[GNU Lesser General Public License v3.0](https://www.gnu.org/licenses/lgpl-3.0.html).
