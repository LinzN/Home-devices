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
import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
import org.json.JSONObject;

public class GET_DeviceStatus implements IRequest {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public GET_DeviceStatus(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object proceedRequestData(RequestData requestData) {
        String deviceName = requestData.getSubChannels().get(0);
        TasmotaMQTTDevice tasmotaDevice = this.homeDevicesPlugin.getTasmotaDevice(deviceName);
        JSONObject jsonObject = new JSONObject();
        if (tasmotaDevice.deviceStatus != null) {
            jsonObject.put("status", tasmotaDevice.getDeviceStatus());
        } else {
            jsonObject.put("error", 404);
        }
        return jsonObject;
    }

    @Override
    public String name() {
        return "device-status";
    }
}
