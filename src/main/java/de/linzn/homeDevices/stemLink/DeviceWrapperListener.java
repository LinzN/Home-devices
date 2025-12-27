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
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.stem.STEMApp;
import de.linzn.stemLink.components.events.ReceiveDataEvent;
import de.linzn.stemLink.components.events.handler.EventHandler;


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
            STEMApp.LOGGER.ERROR(e);
        }
        STEMApp.getInstance().getStemLinkModule().getStemLinkServer().getClients().values().forEach(serverConnection -> serverConnection.writeOutput(headerChannel, byteArrayOutputStream.toByteArray()));
        STEMApp.LOGGER.INFO("Publish device update [" + configName + "] to STEMLINK network");
    }

    @EventHandler(channel = "switchable_device")
    public void onReceiveEvent(ReceiveDataEvent event) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getDataInBytes()));
        try {
            String configName = in.readUTF();

            String action = in.readUTF();
            MqttSwitch mqttSwitch = (MqttSwitch) homeDevicesPlugin.getDeviceManager().getMqttDevice(configName);

            if (action.equalsIgnoreCase("switch_status")) {
                boolean value = in.readBoolean();
                mqttSwitch.switchDevice(value);
            } else if (action.equalsIgnoreCase("toggle_status")) {
                mqttSwitch.toggleDevice();
            }

        } catch (IOException e) {
            STEMApp.LOGGER.ERROR(e);
        }

    }

}
