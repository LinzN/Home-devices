/*
 * Copyright (C) 2021. Niklas Linz - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the LGPLv3 license, which unfortunately won't be
 * written for another century.
 *
 * You should have received a copy of the LGPLv3 license with
 * this file. If not, please write to: niklas.linz@enigmar.de
 *
 */

package de.linzn.homeDevices.restfulapi.get;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
import org.json.JSONObject;


public class GET_AutoMode implements IRequest {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public GET_AutoMode(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object proceedRequestData(RequestData requestData) {
        JSONObject jsonObject = new JSONObject();

        for (SwitchCategory switchCategory : SwitchCategory.values()) {
            boolean status = this.homeDevicesPlugin.isCategoryInAutoSwitchOffMode(switchCategory);
            jsonObject.put(switchCategory.name(), status);
        }
        return jsonObject;
    }

    @Override
    public Object genericData() {
        return proceedRequestData(null);
    }

    @Override
    public String name() {
        return "automode";
    }
}
