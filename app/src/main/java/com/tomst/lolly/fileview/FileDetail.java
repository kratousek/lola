package com.tomst.lolly.fileview;


public class FileDetail
{
    private String name;
    private int iconID;
    private int FileSize;
    private String fullName;


    public FileDetail(String filename, int iconID)
    {
        this.name = filename;
        this.iconID = iconID;
        this.fullName =  "";
    }


    public FileDetail(String filename, String FullName,  int iconID)
    {
        this.name = filename;
        this.iconID = iconID;
        this.fullName = FullName;
    }


    public String getName() { return name; }


    public int getIconID() { return iconID; }


    public String getFull() { return fullName; }


    public int getFileSize() { return FileSize; }
}
