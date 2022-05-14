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
import de.linzn.homeDevices.devices.switches.SwitchableMQTTDevice;
import de.linzn.homeDevices.events.RestApiSwitchRequestEvent;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
import de.stem.stemSystem.STEMSystemApp;
import org.json.JSONObject;

public class POST_ChangeDevice implements IRequest {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public POST_ChangeDevice(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object proceedRequestData(RequestData requestData) {
        JSONObject jsonObject = new JSONObject();

        String deviceName = requestData.getSubChannels().get(0);
        SwitchableMQTTDevice switchableMQTTDevice = this.homeDevicesPlugin.getSwitchableMQTTDevice(deviceName);

        RestApiSwitchRequestEvent restApiSwitchRequestEvent = new RestApiSwitchRequestEvent(switchableMQTTDevice);
        STEMSystemApp.getInstance().getEventModule().getStemEventBus().fireEvent(restApiSwitchRequestEvent);

        if (!restApiSwitchRequestEvent.isCanceled()) {
            boolean newStatus;

            if (requestData.getSubChannels().size() < 2) {
                switchableMQTTDevice.toggleDevice();
                newStatus = switchableMQTTDevice.getDeviceStatus();
                STEMSystemApp.LOGGER.INFO("[REST] Request device toggle " + deviceName + "#->#" + requestData.getInetSocketAddress().getAddress().getHostName());

            } else {
                boolean setStatus = Boolean.parseBoolean(requestData.getSubChannels().get(1));
                switchableMQTTDevice.switchDevice(setStatus);
                newStatus = switchableMQTTDevice.getDeviceStatus();
                STEMSystemApp.LOGGER.INFO("[REST] Request device switch " + deviceName + ":::" + setStatus + "#->#" + requestData.getInetSocketAddress().getAddress().getHostName());
            }

            jsonObject.put("status", newStatus);
        }
        return jsonObject;
    }

    @Override
    public String name() {
        return "change-device-status";
    }
}
