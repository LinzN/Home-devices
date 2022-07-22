package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
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
        STEMSystemApp.LOGGER.DEBUG("MQTTData HomeDevices WEBAPI:");
        STEMSystemApp.LOGGER.DEBUG(postData);

        String mqttDeviceName = postData.get("deviceName").toString();
        String requestAction = postData.get("requestAction").toString();

        MqttDevice mqttDevice = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getMqttDevice(mqttDeviceName);
        if (mqttDevice != null) {
            if (requestAction.equalsIgnoreCase("READ")) {
                jsonObject = mqttDevice.getJSONData();
            } else if (requestAction.equalsIgnoreCase("WRITE")) {
                jsonObject = mqttDevice.setJSONData(postData);
            }
        } else {
            jsonObject.put("error", 404);
        }
        return jsonObject;
    }
}
