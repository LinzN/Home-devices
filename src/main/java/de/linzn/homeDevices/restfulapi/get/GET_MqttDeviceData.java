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
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.restfulapi.api.jsonapi.IRequest;
import de.linzn.restfulapi.api.jsonapi.RequestData;
import org.json.JSONObject;

public class GET_MqttDeviceData implements IRequest {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public GET_MqttDeviceData(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    @Override
    public Object proceedRequestData(RequestData requestData) {
        String mqttDeviceName = requestData.getSubChannels().get(0);
        JSONObject jsonObject = new JSONObject();

        MqttDevice mqttDevice = this.homeDevicesPlugin.getDeviceManager().getMqttDevice(mqttDeviceName);
        if (mqttDevice != null) {
            jsonObject = mqttDevice.getJSONData();
        } else {
            jsonObject.put("error", 404);
        }
        return jsonObject;
    }

    @Override
    public String name() {
        return "mqtt-device-data";
    }
}
