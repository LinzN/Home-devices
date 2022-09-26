package de.linzn.homeDevices.healthcheck;

import de.linzn.homeDevices.HomeDevicesPlugin;
import de.linzn.homeDevices.devices.interfaces.MqttDevice;
import de.linzn.homeDevices.devices.interfaces.MqttSensor;
import de.linzn.homeDevices.devices.interfaces.MqttSwitch;
import de.linzn.homeDevices.devices.other.PowerConsumption;
import de.linzn.homeDevices.devices.other.ZigbeeThermostatDevice;
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
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        try {
            Thread.sleep(1000*120);
        } catch (InterruptedException ignored) {
        }
        HealthCheckFeedback healthCheckFeedback;
        for (MqttDevice device : devices) {
            if (device.healthCheckStatus()) {
                healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.DONE, "Device status ok");
            } else {
                STEMSystemApp.LOGGER.WARNING("Check failed for " + device.getConfigName());
                if (device instanceof MqttSensor) {
                    healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.WARNING, "Device status warning");
                } else if (device instanceof MqttSwitch) {
                    healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device status error");
                } else if (device instanceof ZigbeeThermostatDevice) {
                    healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device status error");
                } else if (device instanceof PowerConsumption) {
                    healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.WARNING, "Device status warning");
                } else {
                    healthCheckFeedback = new HealthCheckFeedback(HealthCheckLevel.ERROR, "Device status unknown");
                }
            }
            this.addHealthCheckFeedback(healthCheckFeedback);
        }
    }
}
