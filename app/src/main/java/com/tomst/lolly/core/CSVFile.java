package com.tomst.lolly.core;


import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class CSVFile
{
    private static final String TAG = "CSV";
    private static final char WRITE_PERM = 'w';
    private static final char READ_PERM = 'r';

    // operational members
    private final char permission;
    private FileWriter writer;
    private FileReader reader;


    private CSVFile(String file_name, char permission) throws IOException
    {
        this.permission = permission;

        if (permission == WRITE_PERM)
        {
            this.writer = new FileWriter(file_name);
        }
        else
        {
            this.reader = new FileReader(file_name);
        }
    }


    // destructor allows closing of open file at point of destruction
    protected void finalize() throws IOException
    {
        if (this.permission == WRITE_PERM)
        {
            this.writer.close();
        }
        else
        {
            this.reader.close();
        }
    }


    // opening, closing, and creating new files
    public static CSVFile create(String file_name)
    {
        CSVFile csv_file = null;

        try
        {
            File new_file = new File(file_name);

            if (new_file.createNewFile())
            {
                Log.d(TAG, "Created file named: " + file_name);

                csv_file = open(file_name, WRITE_PERM);
            }
            else
            {
                Log.d(TAG, file_name + " file already exists.");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return csv_file;
    }


    public static CSVFile open(String file_name, char permission)
    {
        CSVFile csv_file = null;

        try
        {
            csv_file = new CSVFile(file_name, permission);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return csv_file;
    }


    public void close()
    {
        try
        {
            if (this.permission == WRITE_PERM)
            {
                this.writer.close();
            }
            else
            {
                this.reader.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    // reading and writing to files
    public void write(String buffer)
    {
    }


    public String readLine()
    {
        return "";
    }


    public String readAllLines()
    {
        return "";
    }
}
