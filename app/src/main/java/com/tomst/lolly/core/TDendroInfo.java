package com.tomst.lolly.core;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;

public class TDendroInfo
{
    public String serial;
    // include picture later
    public Long startLine, endLine, longitude, latitude;
    // an array of measurements (lines of a file)
    public ArrayList<TMereni> mers = new ArrayList<TMereni>();
    public ArrayList<Entry> vT1 = new ArrayList<>();
    public ArrayList<Entry> vT2 = new ArrayList<>();
    public ArrayList<Entry> vT3 = new ArrayList<>();
    public ArrayList<Entry> vHA = new ArrayList<>();


    public TDendroInfo(TDendroInfo header)
    {
        this.serial = header.serial;
        this.startLine = header.startLine;
        this.endLine = header.endLine;
        this.longitude = header.longitude;
        this.latitude = header.latitude;
        this.mers = header.mers;
    }
}
