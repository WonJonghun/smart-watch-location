package com.iplus.watch_beacon_app.activity;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import com.iplus.watch_beacon_app.constant.Constant;
import com.iplus.watch_beacon_app.constant.Mqtt;
import com.iplus.watch_beacon_app.domain.BeaconData;
import com.iplus.watch_beacon_app.enumerate.Status;
import com.iplus.watch_beacon_app.databinding.ActivityMainBinding;
import com.iplus.watch_beacon_app.domain.RequestData;
import com.iplus.watch_beacon_app.util.KalmanFilter;
import com.iplus.watch_beacon_app.util.MyMqttClient;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.InternalBeaconConsumer;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends WearableActivity implements SensorEventListener,
    InternalBeaconConsumer {

  private TextView locationTextView;
  private TextView heartRateTextView;
  private TextView statusTextView;
  private TextView ambientTextView;
  private LinearLayout normalLayout;
  private ActivityMainBinding binding;
  private SensorManager sensorManager;
  private BeaconManager mBeaconManager;
  private Vibrator vibrator;
  private Float mHeartRate = 0F;
  private Status currentStatus = Status.RUNNING;
  private KalmanFilter kalmanFilter = new KalmanFilter();
  private String watchId;

  private MyMqttClient myMqttClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setAmbientEnabled();
    watchId = Settings.Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    mBeaconManager = BeaconManager.getInstanceForApplication(this);
    mBeaconManager.getBeaconParsers().add(
        new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
    mBeaconManager.setBackgroundModeInternal(false);
    mBeaconManager.setEnableScheduledScanJobs(false);
    mBeaconManager.setForegroundBetweenScanPeriod(0);
    mBeaconManager.setForegroundScanPeriod(1800);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    normalLayout = binding.normalLayout;
    statusTextView = binding.textStatus;
    ambientTextView = binding.textAmbient;
    ambientTextView.getPaint().setAntiAlias(false);
    locationTextView = binding.textLocation;
    heartRateTextView = binding.textHeartRate;

    myMqttClient = new MyMqttClient(Mqtt.USERNAME, Mqtt.PASSWORD, Mqtt.SERVER_URI, Mqtt.clientId,
        new MyMqttCallback());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!checkPermission()) {
      return;
    }
    mBeaconManager.bindInternal(this);
    sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE),
        SensorManager.SENSOR_DELAY_NORMAL);
    screenUpdate(Status.RUNNING);
  }

  @Override
  protected void onPause() {
    super.onPause();

    mBeaconManager.unbindInternal(this);
    sensorManager.unregisterListener(this);
  }

  @Override
  public void onEnterAmbient(Bundle ambientDetails) {
    super.onEnterAmbient(ambientDetails);
    ambientTextView.setVisibility(View.VISIBLE);
    normalLayout.setVisibility(View.INVISIBLE);
  }

  @Override
  public void onExitAmbient() {
    super.onExitAmbient();
    ambientTextView.setVisibility(View.INVISIBLE);
    normalLayout.setVisibility(View.VISIBLE);
  }

  private boolean checkPermission() {
    List<String> permissionList = new ArrayList<>();
    for (String p : Constant.PERMISSIONS) {
      if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
        permissionList.add(p);
      }
    }
    if (!permissionList.isEmpty()) {
      requestPermissions(permissionList.toArray(new String[permissionList.size()]),
          Constant.REQUEST_CODE_PERMISSION);
      return false;
    }
    return true;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    if (requestCode == Constant.REQUEST_CODE_PERMISSION && grantResults.length > 0) {
      for (int i = 0; i < grantResults.length; i++) {
        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
          finish();
        }
      }
    }
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
      mHeartRate = sensorEvent.accuracy > 0 ? sensorEvent.values[0] : 0;
      heartRateTextView.setText(String.format("%.0f bpm", mHeartRate));
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {

  }


  @Override
  public void onBeaconServiceConnect() {
    mBeaconManager.removeAllRangeNotifiers();
    mBeaconManager.addRangeNotifier((result, region) -> {
      if (result.isEmpty()) {
        return;
      }

      //major값 기준 매핑
      Map<Identifier, List<Beacon>> beaconMap = result.stream()
          .collect(Collectors.groupingBy(Beacon::getId2));

      //가장 많은 수의 major 비콘
      Identifier major = beaconMap.entrySet().stream()
          .max(Comparator.comparingInt(entry -> entry.getValue().size())).map(Entry::getKey).get();
      List<Beacon> beacons = beaconMap.get(major);

      double[][] pos = new double[beacons.size()][2];
      double[] dist = new double[beacons.size()];

      // minor값 기준 오름차순 정렬
      List<Beacon> sortedBeacons = beacons.stream()
          .sorted(Comparator.comparingInt(beacon -> beacon.getId3().toInt()))
          .collect(Collectors.toList());

      int i = 0;
      for (Iterator<Beacon> iterator = sortedBeacons.iterator(); iterator.hasNext(); i++) {
        Beacon beacon = iterator.next();
        pos[i] = new double[]{(beacon.getId3().toInt() - sortedBeacons.get(0).getId3().toInt())
            * Constant.BEACON_BETWEEN_DISTANCE, 0};

        dist[i] = beacon.getDistance();
      }

      try {
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(
            new TrilaterationFunction(pos, dist), new LevenbergMarquardtOptimizer());
        Optimum optimum = solver.solve();

        // the answer
        double[] centroid = optimum.getPoint().toArray();

        double distance = kalmanFilter
            .update(sortedBeacons.get(0).getId3().toInt(), centroid[0]);
        locationTextView.setText(String.format("%.1f m", distance));

        List<Integer> minorList = sortedBeacons.stream()
            .sorted(Comparator.comparingDouble(beacon -> beacon.getDistance())).limit(2)
            .map(beacon -> beacon.getId3().toInt()).collect(Collectors.toList());

        RequestData requestData = new RequestData(watchId, major.toInt(), minorList, distance, mHeartRate);

        //mqtt
        if (myMqttClient.isConnected()) {
          try {
            myMqttClient.publish(Mqtt.TOPIC, new Gson().toJson(requestData));
          } catch (MqttException e) {
            e.printStackTrace();
          }
        }
      } catch (IllegalArgumentException e) {
        return;
      }
    });

    mBeaconManager.startRangingBeacons(new Region("ranging",
        Identifier.fromUuid(UUID.fromString("e2c56db5-dffb-48d2-b060-d0f5a71096e0")),
        null, null));
  }

  private void screenUpdate(Status status) {
    currentStatus = status;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        statusTextView.setText(status.getText());
        statusTextView.setTextColor(status.getColor());
      }
    });
  }

  private void connectionError() {
    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
    screenUpdate(Status.NETWORL_ERROR);
  }

  private class MyMqttCallback implements MqttCallback {

    @Override
    public void connectionLost(Throwable throwable) {
      Log.d("MQTT", "connectionLost");
      connectionError();
      throwable.printStackTrace();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
      Log.d("MQTT", "messageArrived");
      System.out.println(s);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
      Log.d("MQTT", "deliveryComplete");
      screenUpdate(Status.RUNNING);
    }
  }
}