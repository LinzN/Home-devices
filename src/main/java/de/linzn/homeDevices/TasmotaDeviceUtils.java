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

package de.linzn.homeDevices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class TasmotaDeviceUtils {

    public static DeviceStatus readDeviceStatus(String hostname) {
        String apiString = "http://" + hostname + "/cm?cmnd=Power";
        JSONObject object = readJsonFromUrl(apiString);
        if (object == null) {
            return DeviceStatus.OFFLINE;
        }
        return object.getString("POWER").equalsIgnoreCase("ON") ? DeviceStatus.ENABLED : DeviceStatus.DISABLED;
    }

    private static JSONObject readJsonFromUrl(String url) throws JSONException {
        try {
            URLConnection con = new URL(url).openConnection();
            con.setConnectTimeout(1500);
            con.setReadTimeout(1500);

            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
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

    public static DeviceStatus toggleDevice(String hostname) {
        String apiString = "http://" + hostname + "/cm?cmnd=Power%20toggle";
        JSONObject object = readJsonFromUrl(apiString);
        if (object == null) {
            return DeviceStatus.OFFLINE;
        }
        return object.getString("POWER").equalsIgnoreCase("ON") ? DeviceStatus.ENABLED : DeviceStatus.DISABLED;
    }

    public static DeviceStatus setDeviceStatus(String hostname, boolean status) {
        String apiString = "http://" + hostname + "/cm?cmnd=Power%20";
        if (status) {
            apiString = apiString + "on";
        } else {
            apiString = apiString + "off";
        }
        JSONObject object = readJsonFromUrl(apiString);
        if (object == null) {
            return DeviceStatus.OFFLINE;
        }
        return object.getString("POWER").equalsIgnoreCase("ON") ? DeviceStatus.ENABLED : DeviceStatus.DISABLED;
    }

    public static boolean checkLightShutdown(long lastEnabled, String timedStart, String timedStop, int timedOffsetMinutes) {
        long currentTime = System.currentTimeMillis();

        long startLong = 0;
        long stopLong = 0;
        // if (currentTime > startLong && currentTime < stopLong) {
        if (lastEnabled != -1) {
            if ((currentTime - lastEnabled) > (TimeUnit.MINUTES.toMillis(timedOffsetMinutes))) {
                System.out.println("Offset is correct.");
                return true;
            }
            //    }
        }
        return false;
    }

}
