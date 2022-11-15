package com.iplus.watch_beacon_app.util;

import java.util.Objects;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MyMqttClient {

  private String serverURI;
  private String clientId;
  private String username;
  private String password;
  private MqttClient client;
  private MqttCallback callback;
  private MqttConnectOptions option;

  public MyMqttClient(String username, String password, String serverURI, String clientId, MqttCallback callback) {
    this.username = username;
    this.password = password;
    this.serverURI = serverURI;
    this.clientId = clientId;
    this.callback = callback;
  }

  public boolean connect() {
    this.option = new MqttConnectOptions();
    this.option.setCleanSession(true);
    this.option.setConnectionTimeout(30);
    this.option.setKeepAliveInterval(30);
    this.option.setUserName(this.username);
    this.option.setPassword(this.password.toCharArray());

    try {
      this.client = new MqttClient(this.serverURI, this.clientId, new MemoryPersistence());
      this.client.setCallback(callback);
      this.client.connect(this.option);
    } catch (MqttException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public boolean isConnected() {
    if (Objects.isNull(client) || !client.isConnected()) {
      return connect();
    }

    return true;
  }

  public boolean subscribe(String... topics) {
    try {
      if (topics != null) {
        for (String topic : topics) {
          client.subscribe(topic, 0);  //구독할 주제, 숫자는 품질 값
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public boolean publish(String topic, String msg) throws MqttPersistenceException, MqttException {
    MqttMessage message = new MqttMessage();
    message.setPayload(msg.getBytes());  //보낼 메시지
    client.publish(topic, message);  //토픽과 함께 보낸다.
    return false;
  }
}
