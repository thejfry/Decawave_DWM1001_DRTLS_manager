package com.decawave.argomanager.components.struct;

import android.os.Parcel;
import android.os.Parcelable;

public class Anchor implements Parcelable {
    private String name;
    private float x;
    private float y;

    public Anchor() {
    }

    public String getName() {
        return name;
    }

    public float getAnchorX() {
        return x;
    }

    public float getAnchorY() {
        return y;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAnchorX(float x) {
        this.x = x;
    }

    public void setAnchorY(float y) {
        this.y = y;
    }


    protected Anchor(Parcel in) {
        name = in.readString();
        x = in.readFloat();
        y = in.readFloat();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeFloat(x);
        dest.writeFloat(y);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Anchor> CREATOR = new Parcelable.Creator<Anchor>() {
        @Override
        public Anchor createFromParcel(Parcel in) {
            return new Anchor(in);
        }

        @Override
        public Anchor[] newArray(int size) {
            return new Anchor[size];
        }
    };
}

//public class Anchor {
//    private String name;
//    private float x;
//    private float y;
//
//    public Anchor() {
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public float getAnchorX() {
//        return x;
//    }
//
//    public float getAnchorY() {
//        return y;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public void setAnchorX(float x) {
//        this.x = x;
//    }
//
//    public void setAnchorY(float y) {
//        this.y = y;
//    }
//
//}
