package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MqttDeviceWebAPI extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();
        JSONObject postData = httpRequestClientPayload.getPostJson();
        HomeDevicesPlugin.homeDevicesPlugin.getPluginLogger().debug("MQTTData HomeDevices WEBAPI: " + postData);

        String mqttDeviceName = postData.optString("deviceName", null);
        String requestAction = postData.optString("requestAction", "");
        MqttDevice mqttDevice = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getMqttDevice(mqttDeviceName);

        if (mqttDevice == null) {
            apiResponse.setError("No device found with this name!");
        } else if (requestAction.equalsIgnoreCase("READ")) {
            apiResponse.setFullContent(mqttDevice.getJSONData());
        } else if (requestAction.equalsIgnoreCase("WRITE")) {
            try {
                apiResponse.setFullContent(mqttDevice.setJSONData(postData));
            } catch (JSONException e) {
                apiResponse.setError("Invalid device data: " + e.getMessage());
            }
        } else {
            apiResponse.setError("No restAction defined. Use 'READ' or 'WRITE'");
        }
        return apiResponse.buildResponse();
    }
}
