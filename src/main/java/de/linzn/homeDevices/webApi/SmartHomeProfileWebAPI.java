package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class SmartHomeProfileWebAPI extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        JSONObject jsonObject = new JSONObject();
        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();

        String requestAction = postData.get("requestAction").toString();

        if (requestAction.equalsIgnoreCase("READ")) {
            if (postData.has("current")) {
                jsonObject.put("currentProfile", HomeDevicesPlugin.homeDevicesPlugin.getCurrentProfile().name());
            } else if (postData.has("available")) {
                JSONArray values = new JSONArray();
                values.putAll(SmartHomeProfile.valuesToString());
                jsonObject.put("availableProfiles", values);
            }

        } else if (requestAction.equalsIgnoreCase("WRITE")) {
            if (postData.has("profile")) {
                if (SmartHomeProfile.has(postData.getString("profile"))) {
                    SmartHomeProfile smartHomeProfile = SmartHomeProfile.valueOf(postData.getString("profile"));
                    if(HomeDevicesPlugin.homeDevicesPlugin.changeSmartHomeProfile(smartHomeProfile)){
                        jsonObject.put("error", 0);
                    } else {
                        jsonObject.put("error", 1);
                    }
                } else {
                    jsonObject.put("error", 404);
                }
            } else {
                jsonObject.put("error", 404);
            }
        }

        return jsonObject;
    }
}
