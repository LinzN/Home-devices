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

package de.linzn.homeDevices.restfulapi.get;

import de.linzn.homeDevices.DeviceStatus;
import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.TasmotaDevice;
import de.linzn.restfulapi.api.jsonapi.get.IGetJSON;
import org.json.JSONObject;

import java.util.List;

public class GET_DeviceStatus implements IGetJSON {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public GET_DeviceStatus(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object getRequestData(List<String> inputList) {
        String deviceName = inputList.get(1);
        TasmotaDevice tasmotaDevice = this.homeDevicesPlugin.getTasmotaDevice(deviceName);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", tasmotaDevice.getDeviceStatus() == DeviceStatus.ENABLED);
        return jsonObject;
    }

    @Override
    public Object getGenericData() {
        return null;
    }

    @Override
    public String name() {
        return "device-status";
    }
}
