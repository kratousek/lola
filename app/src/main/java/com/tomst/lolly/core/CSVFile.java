package com.tomst.lolly.core;


import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class CSVFile
{
    private static final String TAG = "CSV";
    private static final char WRITE_MODE = 'w';
    private static final char READ_MODE = 'r';

    // operational members
    private final char mode;
    private final String path;
    private FileWriter writer;
    private FileReader reader;


    /**
     * Instantiates a new CSVFile with which a CSV file's contents can be
     * written or read.
     *
     * @param path The path at which to open a file, including the file name
     * @param mode Specifies the file will be written to or read from
     * @throws IOException
     */
    private CSVFile(String path, char mode)
    {
        this.mode = mode;
        this.path = path;

        try
        {
            if (mode == WRITE_MODE)
            {
                this.writer = new FileWriter(path);
            }
            else
            {
                this.reader = new FileReader(path);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    // destructor allows closing of open file at point of destruction
    protected void finalize() throws IOException
    {
        if (this.mode == WRITE_MODE)
        {
            this.writer.close();
        }
        else
        {
            this.reader.close();
        }
    }


    // opening, closing, and creating new files

    /**
     * Creates a new file in the filesystem. Shorthand for creating a file and
     * calling CSVFile.open().
     *
     * @param path The path at which to create the file, including the file name
     * @return Reference to a CSVFile through which a file can be interacted
     */
    public static CSVFile create(String path)
    {
        CSVFile csv_file = null;

        try
        {
            File new_file = new File(path);

            if (new_file.createNewFile())
            {
                Log.d(TAG, "Created file named: " + path);

                csv_file = open(path, WRITE_MODE);
            }
            else
            {
                Log.d(TAG, path + " file already exists.");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return csv_file;
    }


    /**
     * Opens a file for IO operations. The file specified must exist.
     *
     * @param path The path at which to open the file, include the file name
     * @param mode Specifies the file will be written to or read from
     * @return Reference to a CSVFile through which a file can be interacted
     */
    public static CSVFile open(String path, char mode)
    {
        return new CSVFile(path, mode);
    }


    /**
     * Closes a CSVFile object.
     *
     * @apiNote This function can be called directly; otherwise, the garbage
     * collector calls this function on destruction
     */
    public void close()
    {
        try
        {
            if (this.mode == WRITE_MODE)
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

    /**
     * Writes to a file.
     *
     * @param buffer Contents to be written
     */
    public void write(String buffer)
    {
        try
        {
            if (this.mode == WRITE_MODE)
            {
                this.writer.write(buffer);
            }
            else
            {
                Log.e(TAG, this.path + " was not open in write mode!");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Reads the next line from file.
     *
     * @return Line read from the file
     */
    public String readLine()
    {
        return "";
    }


    /**
     * Reads contents of a file.
     *
     * @return Contents of a file
     */
    public String readAllLines()
    {
        return "";
    }
}
