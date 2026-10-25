package de.linzn.homeDevices.devices.sensors;

import de.linzn.homeDevices.devices.enums.MqttDeviceCategory;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.homeDevices.profiles.DeviceProfile;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import org.json.JSONObject;

public class ZigbeeEnvironmentSensor extends EnvironmentSensor {

    public ZigbeeEnvironmentSensor(STEMPlugin stemPlugin, DeviceProfile deviceProfile) {
        super(stemPlugin, deviceProfile, "tele/" + deviceProfile.getZigbeeGateway() + "/" + deviceProfile.getDeviceHardAddress() + "/SENSOR");
    }

    @Override
    public void request_initial_status() {
        this.requestUntilData(() -> this.publish("cmnd/" + deviceProfile.getZigbeeGateway() + "/zbinfo", this.getDeviceHardAddress()),
                MqttDeviceCategory.SENSOR.name());
        if (this.isRunning() && !this.hasData()) {
            /* placeholder values, so dependent displays keep working until the sensor reports */
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Temperature", 0);
            jsonObject.put("Humidity", 0);
            jsonObject.put("BatteryPercentage", 100);
            this.update_data(jsonObject);
        }
    }

    @Override
    public void mqttMessageEvent(String topic, String payload) {
        JSONObject jsonPayload = new JSONObject(payload);
        String address = deviceProfile.getDeviceHardAddress();
        for (String key : new String[]{"ZbReceived", "ZbInfo"}) {
            JSONObject section = jsonPayload.optJSONObject(key);
            if (section != null && section.optJSONObject(address) != null) {
                this.update_data(section.getJSONObject(address));
            }
        }
    }
}
