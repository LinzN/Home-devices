package de.linzn.homeDevices.webApi;

import org.json.JSONObject;

public class WebApiResponseBuilder {

    private boolean hasApiError;
    private String errorDescription;
    private JSONObject content;

    public WebApiResponseBuilder() {
        this.hasApiError = false;
        this.errorDescription = "";
        this.content = new JSONObject();
    }

    public void setError(String errorDescription) {
        this.hasApiError = true;
        this.errorDescription = errorDescription;
    }

    public JSONObject getContent() {
        return this.content;
    }

    public void setContent(JSONObject jsonObject) {
        this.content = jsonObject;
    }

    public JSONObject getResponse() {

        if (hasApiError) {
            this.content.put("apiError", true);
            this.content.put("errorDescription", errorDescription);
        } else {
            this.content.put("apiError", false);
        }

        return this.content;
    }
}
