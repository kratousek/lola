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
    // public constants
    /**
     * Modes allow control over how to open a file. These are useful for
     * deciphering if a file is intended to be overwritten or appended to.
     */
    public static final char APPEND_MODE = 'a';
    public static final char READ_MODE = 'r';
    public static final char WRITE_MODE = 'w';

    // private constants
    private static final String TAG = "CSV";
    private static final int LINE_LENGTH = 0;

    // operational members
    private final char mode;
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
    private CSVFile(File file, char mode)
    {
        this.mode = mode;
        this.file = file;

        try
        {
            if (mode == READ_MODE)
            {
                this.reader = new Scanner(file);
            }
            else
            {
                this.writer = new FileOutputStream(file, (mode == APPEND_MODE));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
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
        CSVFile csvFile = null;

        try
        {
            File file = new File(path);

            if (file.createNewFile())
            {
                Log.d(TAG, "Created file named: " + path);

                csvFile = open(path, WRITE_MODE);
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
    public static CSVFile open(String path, char mode)
    {
        File file = new File(path);

        return new CSVFile(file, mode);
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
            if (mode == WRITE_MODE || mode == APPEND_MODE)
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


    /**
     * Removes a file at the given path from the filesystem.
     *
     * @param path File path from which a file is to be removed
     */
    public static void delete(String path)
    {
        if (!new File(path).delete())
        {
            Log.e(TAG, path + " could not be delete!");
        }
    }


    // reading and writing to files
    /**
     * Copies contents of a source file in a destination file given the file
     * paths.
     * @apiNote Provides a more succinct way of a file.
     *
     * @param srcPath File with contents to copy
     * @param destPath File into which source file contents will be copied
     */
    public static void copy(String srcPath, String destPath)
    {
        CSVFile srcFile = CSVFile.open(srcPath, READ_MODE);
        CSVFile destFile = CSVFile.open(destPath, WRITE_MODE);

        String srcContents = srcFile.readAllLines();
        destFile.write(srcContents);
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
        // TODO: maybe should let the user do this?
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
