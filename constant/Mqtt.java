package com.iplus.watch_beacon_app.constant;

import org.eclipse.paho.client.mqttv3.MqttClient;

public class Mqtt {
  public static String SERVER_URI = "tcp://59.25.178.106:9096";
  public static String clientId = MqttClient.generateClientId();
  public static String USERNAME = "winirnd";
  public static String PASSWORD = "winitech@12345";
  public static String TOPIC = "/mq/v1/test/patrol-status";
}
