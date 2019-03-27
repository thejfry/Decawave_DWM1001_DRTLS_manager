package com.decawave.argomanager.components.struct;

import android.os.Parcel;
import android.os.Parcelable;

public class Anchor implements Parcelable {
    private String name;
    private double x, y, z;

    public Anchor() {
    }

    public String getName() {
        return name;
    }

    public double getAnchorX() {
        return x;
    }

    public double getAnchorY() {
        return y;
    }

    public double getAnchorZ() {
        return z;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAnchorX(double x) {
        this.x = x;
    }

    public void setAnchorY(double y) {
        this.y = y;
    }

    public void setAnchorZ(double z) {
        this.z = z;
    }


    protected Anchor(Parcel in) {
        name = in.readString();
        x = in.readDouble();
        y = in.readDouble();
        z = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeDouble(x);
        dest.writeDouble(y);
        dest.writeDouble(z);
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