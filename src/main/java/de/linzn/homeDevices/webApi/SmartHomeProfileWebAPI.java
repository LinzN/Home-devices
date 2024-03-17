package de.linzn.homeDevices.webApi;

import com.sun.net.httpserver.HttpExchange;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SmartHomeProfile;
import de.linzn.webapi.core.ApiResponse;
import de.linzn.webapi.core.HttpRequestClientPayload;
import de.linzn.webapi.modules.RequestInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class SmartHomeProfileWebAPI extends RequestInterface {
    @Override
    public Object callHttpEvent(HttpExchange httpExchange, HttpRequestClientPayload httpRequestClientPayload) throws IOException {
        ApiResponse apiResponse = new ApiResponse();

        JSONObject postData = (JSONObject) httpRequestClientPayload.getPostData();

        String requestAction = postData.get("requestAction").toString();

        if (requestAction.equalsIgnoreCase("READ")) {
            if (postData.has("current")) {
                apiResponse.getJSONObject().put("currentProfile", HomeDevicesPlugin.homeDevicesPlugin.getProfileController().getCurrentProfile().name());
            } else if (postData.has("available")) {
                JSONArray values = new JSONArray();
                values.putAll(SmartHomeProfile.valuesToString());
                apiResponse.getJSONObject().put("availableProfiles", values);
            }

        } else if (requestAction.equalsIgnoreCase("WRITE")) {
            if (postData.has("profile")) {
                if (SmartHomeProfile.has(postData.getString("profile"))) {
                    SmartHomeProfile smartHomeProfile = SmartHomeProfile.valueOf(postData.getString("profile"));
                    if (!HomeDevicesPlugin.homeDevicesPlugin.getProfileController().requestProfileChange(smartHomeProfile)) {
                        apiResponse.setError("Changing of smartHomeProfile failed!");
                    }
                } else {
                    apiResponse.setError("This smartHomeProfile is not defined!");
                }
            } else {
                apiResponse.setError("No profile defined!");
            }
        } else {
            apiResponse.setError("No restAction defined. Use 'READ' or 'WRITE'");
        }

        return apiResponse.buildResponse();
    }
}
