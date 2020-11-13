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
import de.linzn.homeDevices.devices.TasmotaDevice;
import de.linzn.restfulapi.api.jsonapi.post.IPostJSON;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.utils.Color;
import org.json.JSONObject;

import java.util.List;

public class POST_ChangeDevice implements IPostJSON {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public POST_ChangeDevice(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object postDataRequest(List<String> inputList) {
        JSONObject jsonObject = new JSONObject();

        String deviceName = inputList.get(1);
        STEMSystemApp.LOGGER.INFO(Color.GREEN + "[Restful API] Post Request: DeviceControl::" + deviceName);

        TasmotaDevice tasmotaDevice = this.homeDevicesPlugin.getTasmotaDevice(deviceName);
        boolean newStatus;
        if (inputList.size() < 3) {
            tasmotaDevice.toggleDevice();
            newStatus = false; /* todo fix this with return value*/
        } else {
            boolean setStatus = Boolean.parseBoolean(inputList.get(2));
            tasmotaDevice.setDeviceStatus(setStatus);
            newStatus = setStatus; /* todo fix this with return value*/
        }

        jsonObject.put("status", newStatus);

        return jsonObject;
    }

    @Override
    public String name() {
        return "change-device-status";
    }
}
