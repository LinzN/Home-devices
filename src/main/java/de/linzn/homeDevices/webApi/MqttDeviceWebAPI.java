package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SensorCategory;
import de.linzn.homeDevices.devices.interfaces.EnvironmentSensor;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSensor;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

import java.io.IOException;

public class MqttDeviceWebAPI extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();

        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();
        String mqttDeviceName = postData.getString("deviceName");
        String requestAction = postData.getString("requestAction");

        MqttDevice mqttDevice = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getMqttDevice(mqttDeviceName);
        if (mqttDevice != null) {
            if (requestAction.equalsIgnoreCase("READ")) {
                jsonObject = mqttDevice.getJSONData();
            } else if (requestAction.equalsIgnoreCase("WRITE")) {
                if (mqttDevice instanceof MqttSwitch) {
                    MqttSwitch mqttSwitch = (MqttSwitch) mqttDevice;

                    boolean requestStatus = postData.getBoolean("status");
                    mqttSwitch.switchDevice(requestStatus);
                    boolean newStatus = mqttSwitch.getDeviceStatus();
                    jsonObject.put("status", newStatus);
                }
            }
        } else {
            jsonObject.put("error", 404);
        }
        return jsonObject;
    }
}
