package com.tomst.lolly.core;


import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;


/**
 * Wraps file creation and IO operations into a concise interface
 * specifically for operating on TOMST dendrometer datasets.
 */
public class CSVFile
{
    // TODO: uncomment below constants if there is a problem with reading and
    //  writing to a file
    // public constants
    // public static final char WRITE_MODE = 'w';
    // public static final char READ_MODE = 'r';

    // private constants
    private static final String TAG = "CSV";
    private static final int LINE_LENGTH = 0;

    // operational members
    // private final char mode;
    private File file;
    private FileOutputStream writer;
    private Scanner reader;


    /**
     * Instantiates a new CSVFile with which a CSV file's contents can be
     * written or read.
     *
     * @param file File object representing a file in the filesystem
     * @throws IOException
     */
    private CSVFile(File file)
    {
        // this.mode = mode;
        this.file = file;

        try
        {
            this.writer = new FileOutputStream(file);
            this.reader = new Scanner(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    // destructor allows closing of open file at point of destruction
    protected void finalize() throws IOException
    {
        close();
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
        CSVFile csvFile = null;

        try
        {
            File file = new File(path);

            if (file.createNewFile())
            {
                Log.d(TAG, "Created file named: " + path);

                csvFile = open(path);
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

        return csvFile;
    }


    /**
     * Opens a file for IO operations. The file specified must exist.
     *
     * @param path The path at which to open the file, include the file name
     * @return Reference to a CSVFile through which a file can be interacted
     */
    public static CSVFile open(String path)
    {
        File file = new File(path);

        return new CSVFile(file);
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
            this.writer.close();
            this.reader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    public static void delete(String path)
    {
        if (!new File(path).delete())
        {
            Log.e(TAG, path + " could not be delete!");
        }
    }


    // reading and writing to files
    /**
     * Copies contents a file into this file.
     *
     * @param src File from which to copy
     */
    public void copy(CSVFile src)
    {
        String src_contents = src.readAllLines();
        write(src_contents);
    }

    /**
     * Writes to a file.
     *
     * @param buffer Contents to be written
     */
    public void write(String buffer)
    {
        try
        {
            this.writer.write(buffer.getBytes());
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
        if (this.reader.hasNextLine())
        {
            return this.reader.nextLine();
        }

        return "";
    }


    /**
     * Reads contents of a file.
     *
     * @return Contents of a file
     */
    public String readAllLines()
    {
        String line = "";
        String contents = "";

        while((line = readLine()) != "")
        {
            contents += line;
        }

        return contents;
    }
}
