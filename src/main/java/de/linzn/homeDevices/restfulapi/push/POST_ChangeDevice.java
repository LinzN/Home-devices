/*
 * Copyright (C) 2020. Niklas Linz - All Rights Reserved
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
import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
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

        TasmotaMQTTDevice tasmotaDevice = this.homeDevicesPlugin.getTasmotaDevice(deviceName);
        boolean newStatus;
        if (requestData.getSubChannels().size() < 2) {
            tasmotaDevice.toggleDevice();
            newStatus = tasmotaDevice.getDeviceStatus(); /* todo fix this with return value*/
        } else {
            boolean setStatus = Boolean.parseBoolean(requestData.getSubChannels().get(1));
            tasmotaDevice.switchDevice(setStatus);
            newStatus = tasmotaDevice.getDeviceStatus(); /* todo fix this with return value*/
        }

        jsonObject.put("status", newStatus);

        return jsonObject;
    }

    @Override
    public String name() {
        return "change-device-status";
    }
}
