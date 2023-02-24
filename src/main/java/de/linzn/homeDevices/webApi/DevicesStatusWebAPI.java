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
            if (device.hasData()) {
                if (device instanceof MqttSwitch) {
                    MqttSwitch mqttSwitch = (MqttSwitch) device;
                    SwitchCategory switchCategory = mqttSwitch.switchCategory;
                    if (!jsonObject.has("switches")) {
                        jsonObject.put("switches", new JSONObject());
                    }

                    if (!jsonObject.getJSONObject("switches").has(switchCategory.name())) {
                        JSONObject categoryData = new JSONObject();
                        categoryData.put("amount", 0);
                        categoryData.put("running", 0);
                        categoryData.put("notRunning", 0);
                        categoryData.put("values", new JsonArray());
                        jsonObject.getJSONObject("switches").put(switchCategory.name(), categoryData);
                    }
                    jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).getJSONArray("values").put(mqttSwitch.getConfigName());

                    int amount = jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).getInt("amount");
                    jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).remove("amount");
                    amount++;
                    jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).put("amount", amount);

                    if (mqttSwitch.getDeviceStatus()) {
                        int running = jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).getInt("running");
                        jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).remove("running");
                        running++;
                        jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).put("running", running);
                    } else {
                        int notRunning = jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).getInt("notRunning");
                        jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).remove("notRunning");
                        notRunning++;
                        jsonObject.getJSONObject("switches").getJSONObject(switchCategory.name()).put("notRunning", notRunning);
                    }

                }
            }
        }

        return jsonObject;
    }
}
