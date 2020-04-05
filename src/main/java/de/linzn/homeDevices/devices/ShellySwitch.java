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

package de.linzn.homeDevices.devices;

import de.linzn.homeDevices.DeviceStatus;
import de.linzn.homeDevices.HomeDevicesPlugin;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class ShellySwitch {

    private String deviceName;
    private String hostname;

    public ShellySwitch(String deviceName) {
        this.deviceName = deviceName;
        this.hostname = HomeDevicesPlugin.homeDevicesPlugin.getDefaultConfig().getString("shellySwitch." + deviceName.toLowerCase() + ".hostname");
    }

    private static JSONObject readJsonFromUrl(String url) throws JSONException {
        try {
            URLConnection con = new URL(url).openConnection();
            con.setConnectTimeout(1500);
            con.setReadTimeout(1500);

            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public void toggleDevice() {
        String apiString = "http://" + hostname + "/cm?cmnd=Power%20toggle";
        readJsonFromUrl(apiString);
    }

    public void setDeviceStatus(boolean status) {
        String apiString = "http://" + hostname + "/cm?cmnd=Power%20";
        if (status) {
            apiString = apiString + "on";
        } else {
            apiString = apiString + "off";
        }
        readJsonFromUrl(apiString);
    }

    public DeviceStatus getStatus() {
        String apiString = "http://" + hostname + "/cm?cmnd=Power";
        JSONObject object = readJsonFromUrl(apiString);
        if (object == null) {
            return DeviceStatus.OFFLINE;
        }
        return object.getString("POWER").equalsIgnoreCase("ON") ? DeviceStatus.ENABLED : DeviceStatus.DISABLED;
    }
}
