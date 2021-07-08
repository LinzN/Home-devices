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

package de.linzn.homeDevices.stemLink;


import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.TasmotaMQTTDevice;
import de.linzn.stemLink.components.events.ReceiveDataEvent;
import de.linzn.stemLink.components.events.handler.EventHandler;
import de.stem.stemSystem.STEMSystemApp;

import java.io.*;

public class DeviceWrapperListener {

    private final HomeDevicesPlugin homeDevicesPlugin;

    public DeviceWrapperListener(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    public static void updateStatus(String configName, boolean value) {
        String headerChannel = "tasmota_device";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeUTF(configName);
            dataOutputStream.writeUTF("update_status");
            dataOutputStream.writeBoolean(value);
        } catch (IOException e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }
        STEMSystemApp.getInstance().getStemLinkModule().getStemLinkServer().getClients().values().forEach(serverConnection -> serverConnection.writeOutput(headerChannel, byteArrayOutputStream.toByteArray()));
        STEMSystemApp.LOGGER.INFO("Publish device update [" + configName + "] to STEMLINK network");
    }

    @EventHandler(channel = "tasmota_device")
    public void onReceiveEvent(ReceiveDataEvent event) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getDataInBytes()));
        try {
            String configName = in.readUTF();

            String action = in.readUTF();

            if (action.equalsIgnoreCase("switch_status")) {
                boolean value = in.readBoolean();
                TasmotaMQTTDevice tasmotaMQTTDevice = homeDevicesPlugin.getTasmotaDevice(configName);
                tasmotaMQTTDevice.switchDevice(value);
            } else if (action.equalsIgnoreCase("toggle_status")) {
                TasmotaMQTTDevice tasmotaMQTTDevice = homeDevicesPlugin.getTasmotaDevice(configName);
                tasmotaMQTTDevice.toggleDevice();
            }

        } catch (IOException e) {
            STEMSystemApp.LOGGER.ERROR(e);
        }

    }

}
