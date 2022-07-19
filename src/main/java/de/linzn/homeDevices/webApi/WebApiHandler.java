package de.linzn.homeDevices.webApi;

import de.linzn.webapi.modules.WebModule;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

public class WebApiHandler {

    private STEMPlugin stemPlugin;
    private WebModule stemWebModule;

    public WebApiHandler(STEMPlugin stemPlugin){
        this.stemPlugin = stemPlugin;
        stemWebModule = new WebModule("homedevice");
        stemWebModule.registerSubCallHandler(new MqttDeviceWebAPI(), "device");
    }

}
