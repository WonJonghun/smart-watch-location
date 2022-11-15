package com.iplus.watch_beacon_app.util;

import android.util.Log;
import com.iplus.watch_beacon_app.constant.Constant;
import java.time.Instant;

public class KalmanFilter {

  //processNoise > measurementNoise = 새로운 데이터에 민감하게 반응, 변동 폭이 큼
  //processNoise < measurementNoise = 이전 데이터와 비슷하게 반응, 변동이 작음
//  private double Q = 0.00001;   //processNoise
//  private double R = 0.001;     //measurementNoise

  private double Q = 0.006;   //processNoise
  private double R = 0.001;     //measurementNoise
  private double X = 0, P = 1, K;   //X = predict, P = errorCovariance, K == kalmanGain

  private boolean startFlag = true;
  private long startTime;

  public double update(int beaconNumber, double measurement) {
    double beaconDistance = beaconNumber * Constant.BEACON_BETWEEN_DISTANCE;
    double totalDistance = measurement + beaconDistance;
    long now = Instant.now().toEpochMilli();

    //초기화
    if (startFlag) {
      startTime = now;
      X = totalDistance;
      startFlag = false;
      return measurement;
    }

    double movedDistance = totalDistance - X;
    double movedDistancePerSec = movedDistance / ((now - startTime) / 1000.0);

    for (int i = 0; i < ((now - startTime) / 1000); i++) {
      measurementUpdate();
      X = X + movedDistancePerSec * K;
    }

    startTime = now;
    return X;
  }

  private void measurementUpdate() {
    K = (P + Q) / (P + Q + R);
    P = R * (P + Q) / (P + Q + R);
  }
}
