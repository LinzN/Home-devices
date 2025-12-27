package de.linzn.homeDevices.healthcheck;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.stem.STEMApp;
import de.linzn.stem.modules.healthModule.HealthCheck;
import de.linzn.stem.modules.healthModule.HealthCheckFeedback;
import de.linzn.stem.modules.healthModule.HealthCheckLevel;
import de.linzn.stem.modules.pluginModule.STEMPlugin;

import java.util.Collection;

public class HomeDeviceHealthCheck extends HealthCheck {

    public HomeDeviceHealthCheck(STEMPlugin stemPlugin) {
        super(stemPlugin);
    }

    @Override
    protected void runCheckProgress() {
        Collection<MqttDevice> devices = HomeDevicesPlugin.homeDevicesPlugin.getDeviceManager().getAllDevices();
        for (MqttDevice device : devices) {
            device.requestHealthCheck();
            STEMApp.LOGGER.INFO("Request health check for device: " + device.getConfigName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        try {
            Thread.sleep(1000 * 120);
        } catch (InterruptedException ignored) {
        }
        HealthCheckFeedback healthCheckFeedback;
        for (MqttDevice device : devices) {
            if (device.hasData() && device.healthCheckStatus()) {
                healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.DONE, "Device status ok");
            } else {
                STEMApp.LOGGER.WARNING("Check failed for " + device.getConfigName());
                healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device status unknown - " + device.getConfigName());
            }
            this.addHealthCheckFeedback(healthCheckFeedback);
        }
    }
}
