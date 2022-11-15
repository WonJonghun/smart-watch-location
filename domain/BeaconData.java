package com.iplus.watch_beacon_app.domain;

public class BeaconData {
  private String uuid;
  private int major;
  private int minor;
  private int rssi;

  public BeaconData(String uuid, int major, int minor, int rssi) {
    this.uuid = uuid;
    this.major = major;
    this.minor = minor;
    this.rssi = rssi;
  }
}
