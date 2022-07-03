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

package de.linzn.homeDevices.restfulapi.push;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.enums.SwitchCategory;
import de.linzn.homeDevices.events.RestApiAutoModeChangeRequestEvent;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

public class POST_ChangeAutoMode implements IRequest {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public POST_ChangeAutoMode(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object proceedRequestData(RequestData requestData) {
        JSONObject jsonObject = new JSONObject();

        SwitchCategory switchCategory = SwitchCategory.valueOf(requestData.getSubChannels().get(0).toUpperCase());
        boolean value = Boolean.parseBoolean(requestData.getSubChannels().get(1).toLowerCase());
        boolean oldValue = this.homeDevicesPlugin.isCategoryInAutoSwitchOffMode(switchCategory);

        RestApiAutoModeChangeRequestEvent restApiAutoModeChangeRequestEvent = new RestApiAutoModeChangeRequestEvent(switchCategory, oldValue, value);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(restApiAutoModeChangeRequestEvent);

        if (!restApiAutoModeChangeRequestEvent.isCanceled()) {
            STEMSystemApp.LOGGER.INFO("[REST] Request update deviceCategory autoMode " + switchCategory.name() + ":::" + value + "#->#" + requestData.getInetSocketAddress().getAddress().getHostName());
            boolean newValue = this.homeDevicesPlugin.setCategoryInAutoMode(switchCategory, value);
            jsonObject.put("status", newValue);
        }

        return jsonObject;
    }

    @Override
    public String name() {
        return "change-automode";
    }
}
