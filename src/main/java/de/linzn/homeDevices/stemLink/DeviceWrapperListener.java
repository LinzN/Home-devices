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
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.pluginModule.STEMPlugin;
import de.linzn.stemLink.components.events.ReceiveDataEvent;
import de.linzn.stemLink.components.events.handler.EventHandler;

import java.io.*;

/**
 * Bridges switch devices to the StemLink network. StemLink is a core only feature, so this class
 * needs the full STEM dependency.
 */
public class DeviceWrapperListener {

    private static final String HEADER_CHANNEL = "tasmota_device";

    private final HomeDevicesPlugin homeDevicesPlugin;

    public DeviceWrapperListener(HomeDevicesPlugin homeDevicesPlugin) {
        this.homeDevicesPlugin = homeDevicesPlugin;
    }

    /**
     * Sends a status update of a switch to all connected StemLink clients.
     */
    public static void updateStatus(STEMPlugin plugin, String configName, boolean value) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            dataOutputStream.writeUTF(configName);
            dataOutputStream.writeUTF("update_status");
            dataOutputStream.writeBoolean(value);
        } catch (IOException e) {
            plugin.getPluginLogger().error(e);
            return;
        }
        byte[] data = byteArrayOutputStream.toByteArray();
        try {
            STEMApp.getInstance().getStemLinkModule().getStemLinkServer().getClients().values().forEach(serverConnection -> {
                try {
                    serverConnection.writeOutput(HEADER_CHANNEL, data);
                } catch (Exception e) {
                    plugin.getPluginLogger().warning("Unable to send device update to a StemLink client: " + e.getMessage());
                }
            });
            plugin.getPluginLogger().debug("Publish device update [" + configName + "] to STEMLINK network");
        } catch (Exception e) {
            plugin.getPluginLogger().warning("StemLink not available, device update [" + configName + "] not published: " + e.getMessage());
        }
    }

    @EventHandler(channel = "switchable_device")
    public void onReceiveEvent(ReceiveDataEvent event) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getDataInBytes()))) {
            String configName = in.readUTF();
            String action = in.readUTF();
            MqttDevice mqttDevice = homeDevicesPlugin.getDeviceManager().getMqttDevice(configName);
            if (!(mqttDevice instanceof MqttSwitch mqttSwitch)) {
                homeDevicesPlugin.getPluginLogger().warning("StemLink request for unknown switch " + configName);
                return;
            }
            if (action.equalsIgnoreCase("switch_status")) {
                boolean value = in.readBoolean();
                mqttSwitch.switchDevice(value);
            } else if (action.equalsIgnoreCase("toggle_status")) {
                mqttSwitch.toggleDevice();
            } else {
                homeDevicesPlugin.getPluginLogger().warning("Unknown StemLink device action " + action + " for " + configName);
            }
        } catch (IOException e) {
            homeDevicesPlugin.getPluginLogger().error("Invalid StemLink device request", e);
        }
    }
}
