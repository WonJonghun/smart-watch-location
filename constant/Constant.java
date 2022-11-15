package com.iplus.watch_beacon_app.constant;

import android.Manifest.permission;

public class Constant {
  public static final int REQUEST_CODE_PERMISSION = 1023;
  public static final int BEACON_BETWEEN_DISTANCE = 10;

  public static String[] PERMISSIONS = {
      permission.ACCESS_FINE_LOCATION,
      permission.BODY_SENSORS,
  };
}
