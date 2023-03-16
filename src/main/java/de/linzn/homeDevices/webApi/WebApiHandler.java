package de.linzn.homeDevices.webApi;

import de.linzn.webapi.WebApiPlugin;
import de.linzn.webapi.modules.WebModule;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

public class WebApiHandler {

    private final STEMPlugin stemPlugin;
    private final WebModule stemWebModule;

    public WebApiHandler(STEMPlugin stemPlugin) {
        this.stemPlugin = stemPlugin;
        stemWebModule = new WebModule("homedevice");
        stemWebModule.registerSubCallHandler(new MqttDeviceWebAPI(), "device");
        stemWebModule.registerSubCallHandler(new DevicesStatusWebAPI(), "status");
        stemWebModule.registerSubCallHandler(new SmartHomeProfileWebAPI(), "smarthomeprofile");
        WebApiPlugin.webApiPlugin.getWebServer().enableCallModule(stemWebModule);
    }

}
