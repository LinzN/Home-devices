package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class DevicesStatusWebAPI extends RequestInterface {

    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse webApiResponseBuilder = new ApiResponse();
        JSONObject response = webApiResponseBuilder.getJSONObject();
        SmartHomeProfile smartHomeProfile = HomeDevicesPlugin.homeDevicesPlugin.getProfileController().getCurrentProfile();
        response.put("currentProfile", smartHomeProfile.name());
        /* Need to add human presence detector */
        response.put("currentHumanPresence", 0);
        /* Need to add health status */
        response.put("healthStatus", 0);

        JSONObject switches = new JSONObject();
        for (MqttDevice device : HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getAllDevices()) {
            if (!(device instanceof MqttSwitch mqttSwitch) || !device.hasData()) {
                continue;
            }
            String category = mqttSwitch.switchCategory.name();
            JSONObject categoryData = switches.optJSONObject(category);
            if (categoryData == null) {
                categoryData = new JSONObject();
                categoryData.put("amount", 0);
                categoryData.put("running", 0);
                categoryData.put("notRunning", 0);
                categoryData.put("values", new JSONArray());
                switches.put(category, categoryData);
            }
            categoryData.getJSONArray("values").put(mqttSwitch.getConfigName());
            categoryData.put("amount", categoryData.getInt("amount") + 1);
            String counter = mqttSwitch.isSwitchedOn() ? "running" : "notRunning";
            categoryData.put(counter, categoryData.getInt(counter) + 1);
        }
        if (!switches.isEmpty()) {
            response.put("switches", switches);
        }
        return webApiResponseBuilder.buildResponse();
    }
}
