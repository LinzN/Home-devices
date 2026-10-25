package de.linzn.homeDevices.healthcheck;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.stem.modules.healthModule.HealthCheck;
import de.linzn.stem.modules.healthModule.HealthCheckFeedback;
import de.linzn.stem.modules.healthModule.HealthCheckLevel;

import java.util.ArrayList;
import java.util.List;

public class HomeDeviceHealthCheck extends HealthCheck {

    /* time the devices get to answer the health request */
    private static final long ANSWER_TIMEOUT_MS = 120_000;

    private final HomeDevicesPlugin plugin;

    public HomeDeviceHealthCheck(HomeDevicesPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected void runCheckProgress() {
        /* snapshot, the device list must not change between request and evaluation */
        List<MqttDevice> devices = new ArrayList<>(this.plugin.getDeviceManager().getAllDevices());
        try {
            for (MqttDevice device : devices) {
                try {
                    device.requestHealthCheck();
                    this.plugin.getPluginLogger().debug("Request health check for device: " + device.getConfigName());
                } catch (Exception e) {
                    this.plugin.getPluginLogger().error("Health check request failed for " + device.getConfigName(), e);
                }
                /* spread the requests, zigbee gateways drop commands when flooded */
                Thread.sleep(1000);
            }
            Thread.sleep(ANSWER_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            this.addHealthCheckFeedback(new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device health check interrupted"));
            return;
        }

        for (MqttDevice device : devices) {
            HealthCheckFeedback healthCheckFeedback;
            boolean healthy;
            try {
                healthy = device.hasData() && device.healthCheckStatus();
            } catch (Exception e) {
                this.plugin.getPluginLogger().error("Health check failed for " + device.getConfigName(), e);
                healthy = false;
            }
            if (healthy) {
                healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.DONE, "Device status ok - " + device.getConfigName());
            } else {
                this.plugin.getPluginLogger().warning("Check failed for " + device.getConfigName());
                healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device status unknown - " + device.getConfigName());
            }
            this.addHealthCheckFeedback(healthCheckFeedback);
        }
    }
}
