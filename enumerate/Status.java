package com.iplus.watch_beacon_app.enumerate;

import android.graphics.Color;

public enum Status {
  RUNNING("정상 동작", Color.GREEN), ERROR("에러", Color.RED), NETWORL_ERROR("네트워크 에러", Color.RED);

  private String text;
  private int color;

  public String getText() {
    return text;
  }

  public int getColor() {
    return color;
  }

  Status(String text, int color) {
    this.text = text;
    this.color = color;
  }
}
