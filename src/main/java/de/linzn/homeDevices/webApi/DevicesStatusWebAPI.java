package de.linzn.homeDevices.webApi;

import com.mysql.cj.xdevapi.JsonArray;
import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;

public class DevicesStatusWebAPI extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        Collection<MqttDevice> devices = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getAllDevices();

        for (MqttDevice device : devices) {
            if (device instanceof MqttSwitch) {
                MqttSwitch mqttSwitch = (MqttSwitch) device;
                SwitchCategory switchCategory = mqttSwitch.switchCategory;

                if (!jsonObject.has("switches." + switchCategory.name())) {
                    jsonObject.put("switches." + switchCategory.name(), new JsonArray());

                    JSONObject categoryData = new JSONObject();
                    categoryData.put("amount", 0);
                    categoryData.put("running", 0);
                    categoryData.put("notRunning", 0);
                    jsonObject.put("switches.status." + switchCategory, categoryData);
                }
                jsonObject.getJSONArray("switches." + switchCategory.name()).put(mqttSwitch.getConfigName());

                int amount = jsonObject.getJSONObject("switches.status." + switchCategory).getInt("amount");
                jsonObject.getJSONObject("switches.status." + switchCategory).remove("amount");
                amount++;
                jsonObject.getJSONObject("switches.status." + switchCategory).put("amount", amount);

                if (mqttSwitch.getDeviceStatus()) {
                    int running = jsonObject.getJSONObject("switches.status." + switchCategory).getInt("running");
                    jsonObject.getJSONObject("switches.status." + switchCategory).remove("running");
                    running++;
                    jsonObject.getJSONObject("switches.status." + switchCategory).put("running", running);
                } else {
                    int notRunning = jsonObject.getJSONObject("switches.status." + switchCategory).getInt("notRunning");
                    jsonObject.getJSONObject("switches.status." + switchCategory).remove("notRunning");
                    notRunning++;
                    jsonObject.getJSONObject("switches.status." + switchCategory).put("notRunning", notRunning);
                }

            }
        }

        return jsonObject;
    }
}
