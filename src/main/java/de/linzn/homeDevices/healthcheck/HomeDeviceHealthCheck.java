package de.linzn.homeDevices.healthcheck;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.healthModule.HealthCheck;
import de.stem.stemSystem.modules.healthModule.HealthCheckFeedback;
import de.stem.stemSystem.modules.healthModule.HealthCheckLevel;
import de.stem.stemSystem.modules.pluginModule.STEMPlugin;

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
            STEMSystemApp.LOGGER.INFO("Request health check for device: " + device.getConfigName());
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
                STEMSystemApp.LOGGER.WARNING("Check failed for " + device.getConfigName());
                healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device status unknown - " + device.getConfigName());
            }
            this.addHealthCheckFeedback(healthCheckFeedback);
        }
    }
}
