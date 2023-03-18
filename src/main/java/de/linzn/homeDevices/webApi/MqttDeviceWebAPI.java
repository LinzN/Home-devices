package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

import java.io.IOException;

public class MqttDeviceWebAPI extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();
        STEMSystemApp.LOGGER.DEBUG("MQTTData HomeDevices WEBAPI:");
        STEMSystemApp.LOGGER.DEBUG(postData);

        String mqttDeviceName = postData.get("deviceName").toString();
        String requestAction = postData.get("requestAction").toString();

        MqttDevice mqttDevice = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getMqttDevice(mqttDeviceName);


        if (mqttDevice != null) {
            if (requestAction.equalsIgnoreCase("READ")) {
                apiResponse.setFullContent(mqttDevice.getJSONData());
            } else if (requestAction.equalsIgnoreCase("WRITE")) {
                apiResponse.setFullContent(mqttDevice.setJSONData(postData));
            } else {
                apiResponse.setError("No restAction defined. Use 'READ' or 'WRITE'");
            }
        } else {
            apiResponse.setError("No device found with this name!");
        }
        return apiResponse.buildResponse();
    }
}
