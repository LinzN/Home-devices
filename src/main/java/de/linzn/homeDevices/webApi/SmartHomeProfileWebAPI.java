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
        WebApiResponseBuilder webApiResponseBuilder = new WebApiResponseBuilder();

        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();

        String requestAction = postData.get("requestAction").toString();

        if (requestAction.equalsIgnoreCase("READ")) {
            if (postData.has("current")) {
                webApiResponseBuilder.getContent().put("currentProfile", HomeDevicesPlugin.homeDevicesPlugin.getCurrentProfile().name());
            } else if (postData.has("available")) {
                JSONArray values = new JSONArray();
                values.putAll(SmartHomeProfile.valuesToString());
                webApiResponseBuilder.getContent().put("availableProfiles", values);
            }

        } else if (requestAction.equalsIgnoreCase("WRITE")) {
            if (postData.has("profile")) {
                if (SmartHomeProfile.has(postData.getString("profile"))) {
                    SmartHomeProfile smartHomeProfile = SmartHomeProfile.valueOf(postData.getString("profile"));
                    if (!HomeDevicesPlugin.homeDevicesPlugin.changeSmartHomeProfile(smartHomeProfile)) {
                        webApiResponseBuilder.setError("Changing of smartHomeProfile failed!");
                    }
                } else {
                    webApiResponseBuilder.setError("This smartHomeProfile is not defined!");
                }
            } else {
                webApiResponseBuilder.setError("No profile defined!");
            }
        } else {
            webApiResponseBuilder.setError("No restAction defined. Use 'READ' or 'WRITE'");
        }

        return webApiResponseBuilder.getResponse();
    }
}
