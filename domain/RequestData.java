package com.iplus.watch_beacon_app.domain;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class RequestData {

  private String watchId;
  private Long transmissionTimestamp;
  private Integer major;
  private String minor;
  private Double distance;
  private Float heartRate;

  public RequestData(String watchId, Integer major, List<Integer> minorList, Double distance, Float heartRate) {
    this.watchId = watchId;
    this.transmissionTimestamp = new Date().getTime();
    this.major = major;
    this.minor = minorList.stream().map(minor -> String.valueOf(minor)).collect(Collectors.joining(","));
    this.distance = distance;
    this.heartRate = heartRate;
  }
}
