package com.urbit_iot.porton.data.source.internet;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttMessageWrapper {
    private String topic;
    private MqttMessage mqttMessage;

    public MqttMessageWrapper(String topic, MqttMessage mqttMessage) {
        this.topic = topic;
        this.mqttMessage = mqttMessage;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public MqttMessage getMqttMessage() {
        return mqttMessage;
    }

    public void setMqttMessage(MqttMessage mqttMessage) {
        this.mqttMessage = mqttMessage;
    }
}
