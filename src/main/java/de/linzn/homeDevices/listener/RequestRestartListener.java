package de.linzn.homeDevices.listener;

import de.stem.stemSystem.STEMSystemApp;
import de.stem.stemSystem.modules.eventModule.events.StemStartupEvent;
import de.stem.stemSystem.modules.eventModule.handler.StemEventHandler;
import de.stem.stemSystem.modules.eventModule.handler.StemEventPriority;
import de.stem.stemSystem.modules.mqttModule.MqttModule;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class RequestRestartListener {
    private String randomZigbeeGateway;

    public RequestRestartListener(String randomZigbeeGateway) {
        this.randomZigbeeGateway = randomZigbeeGateway;
    }

    @StemEventHandler(priority = StemEventPriority.NORMAL)
    public void onStemStartupCompleteEvent(StemStartupEvent event) {
        STEMSystemApp.LOGGER.INFO("Request zigbee2mqtt data from hub!");
        MqttModule mqttModule = STEMSystemApp.getInstance().getMqttModule();
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(2);
        mqttModule.publish(randomZigbeeGateway + "/bridge/request/restart", mqttMessage);
    }
}
