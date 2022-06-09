package com.dugsolutions.fell.map.gen2;

import com.badlogic.gdx.Gdx;

/**
 * Base calc value which holds a common entry points which all generators use.
 */
public abstract class GenZBase {

  static final String TAG = "GenZBase";

  IGenZ icalc;
  int   count;

  public GenZBase() {
    count = 1;
  }

  public GenZBase(int c) {
    count = c;
  }

  public void gen(IGenZ calc, int startx, int starty, int endx, int endy) {

    icalc = calc;
    count--;

    Gdx.app.log(TAG, "gen(" + startx + ", " + starty + ", " + endx + ", " + endy + ")");
    init(startx, starty, endx, endy);

    for (int y = starty; y <= endy; y++) {
      for (int x = startx; x <= endx; x++) {
        setZ(x, y);
      }
    }
  }

  protected abstract void setZ(int x, int y);

  protected void init(int startx, int starty, int endx, int endy) {
  }

  int getCount() {
    return count;
  }
}
