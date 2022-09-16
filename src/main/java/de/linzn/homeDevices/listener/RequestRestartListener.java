package de.linzn.homeDevices.listener;

import de.linzn.homeDevices.profiles.DeviceProfile;
import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.eventModule.events.StemStartupEvent;
import de.stem.stemSystem.modules.eventModule.handler.StemEventHandler;
import de.stem.stemSystem.modules.eventModule.handler.StemEventPriority;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class RequestRestartListener {

    @StemEventHandler(priority = StemEventPriority.NORMAL)
    public void onStemStartupCompleteEvent(StemStartupEvent event) {
        String zigbeeGateway = DeviceProfile.getDefaultConfig().getString("options.zigbeeGateway");
        STEMSystemApp.LOGGER.INFO("Request zigbee2mqtt data from hub!");
        MqttModule mqttModule = STEMSystemApp.getInstance().getMqttModule();
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        mqttModule.publish(zigbeeGateway + "/bridge/request/restart", mqttMessage);
    }
}
