package de.linzn.homeDevices.webApi;

import com.mysql.cj.xdevapi.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;

public class DevicesStatusWebAPI extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse webApiResponseBuilder = new ApiResponse();

        SmartHomeProfile smartHomeProfile = HomeDevicesPlugin.homeDevicesPlugin.getCurrentProfile();
        webApiResponseBuilder.getJSONObject().put("currentProfile", smartHomeProfile.name());
        /* Need to add human presence detector */
        webApiResponseBuilder.getJSONObject().put("currentHumanPresence", 0);
        /* Need to add health status */
        webApiResponseBuilder.getJSONObject().put("healthStatus", 0);

        Collection<MqttDevice> devices = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getAllDevices();

        for (MqttDevice device : devices) {
            if (device.hasData()) {
                if (device instanceof MqttSwitch) {
                    MqttSwitch mqttSwitch = (MqttSwitch) device;
                    SwitchCategory switchCategory = mqttSwitch.switchCategory;
                    if (!webApiResponseBuilder.getJSONObject().has("switches")) {
                        webApiResponseBuilder.getJSONObject().put("switches", new JSONObject());
                    }

                    if (!webApiResponseBuilder.getJSONObject().getJSONObject("switches").has(switchCategory.name())) {
                        JSONObject categoryData = new JSONObject();
                        categoryData.put("amount", 0);
                        categoryData.put("running", 0);
                        categoryData.put("notRunning", 0);
                        categoryData.put("values", new JsonArray());
                        webApiResponseBuilder.getJSONObject().getJSONObject("switches").put(switchCategory.name(), categoryData);
                    }
                    webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).getJSONArray("values").put(mqttSwitch.getConfigName());

                    int amount = webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).getInt("amount");
                    webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).remove("amount");
                    amount++;
                    webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).put("amount", amount);

                    if (mqttSwitch.getDeviceStatus()) {
                        int running = webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).getInt("running");
                        webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).remove("running");
                        running++;
                        webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).put("running", running);
                    } else {
                        int notRunning = webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).getInt("notRunning");
                        webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).remove("notRunning");
                        notRunning++;
                        webApiResponseBuilder.getJSONObject().getJSONObject("switches").getJSONObject(switchCategory.name()).put("notRunning", notRunning);
                    }

                }
            }
        }
        return webApiResponseBuilder.buildResponse();
    }
}
