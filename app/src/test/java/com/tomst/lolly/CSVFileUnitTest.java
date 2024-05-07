package com.tomst.lolly;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import com.tomst.lolly.core.CSVFile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CSVFileUnitTest
{
    @Mock
    Context MockContext;

    public CSVFile csvFile;

    private final String parallelTestFileName = "test_parallel.csv";
    private final String serialTestFileName = "test_serial.csv";
    private final String serialMockData =
        "3;\n"
        + "7832;1;2;\n"
        + "10421;1;3;\n"
        + "79823;1;4;\n"
        + "7832;\n"
        + "1;2023.05.01 00:15;0;7,123;-200;-200;937;206;0;\n"
        + "10421;\n"
        + "1;2023.05.01 00:30;0;1,234;-200;-200;563;234;0;\n"
        + "79823;\n"
        + "1;2023.05.01 00:30;0;34,028;-200;-200;120;789;0;\n";
    private final String parallelMockData =
        "3;\n"
        + "7832;1;2;\n"
        + "10421;1;3;\n"
        + "79823;1;4;\n"
        + "line number!7832!YYYY.MM.DD HH:MM!Tempurature1!Tempurature2!Tempurature3!Diam_or_Moisture!mvs!8!"
            + "line number!10421!YYYY.MM.DD HH:MM!Tempurature1!Tempurature2!Tempurature3!Diam_or_Moisture!mvs!8!"
            + "line number!79823!YYYY.MM.DD HH:MM!Tempurature1!Tempurature2!Tempurature3!Diam_or_Moisture!mvs!8!\n"
        + "1;2023.05.01 00:15;0;7,123;-200;-200;937;206;0;"
        + "1;2023.05.01 00:30;0;1,234;-200;-200;563;234;0;"
        + "1;2023.05.01 00:30;0;34,028;-200;-200;120;789;0;\n";

    @Before
    public void setup()
    {
        // TODO: just store copies in a folder for testing materials
        if (CSVFile.exists(serialTestFileName))
        {
            CSVFile.delete(serialTestFileName);
        }
        if (CSVFile.exists(parallelTestFileName))
        {
            CSVFile.delete(parallelTestFileName);
        }

        // write serial file
        csvFile = CSVFile.create(serialTestFileName);
        csvFile.write(serialMockData);
        csvFile.close();
        // write parallel file
        csvFile = CSVFile.create(parallelTestFileName);
        csvFile.write(parallelMockData);
        csvFile.close();
    }


    @Test
    public void toParallel_isCorrect()
    {
        final String expectedFileName = "test_serial_parallel.csv";
        if (CSVFile.exists(expectedFileName))
        {
            CSVFile.delete(expectedFileName);
        }

        CSVFile.toParallel(serialTestFileName);

        String actualLines = "";
        CSVFile actualFile =
                CSVFile.open(expectedFileName, CSVFile.READ_MODE);
        String expectedLines = parallelMockData;

        String line = "";
        while ((line = actualFile.readLine()) != "")
        {
            actualLines += line + "\n";
        }

        assertEquals(actualLines, expectedLines);
    }


    @Test
    public void toSerial_isCorrect()
    {
        // TODO: just store copies in a directory for test materials
        if (CSVFile.exists(parallelTestFileName))
        {
            CSVFile.delete(parallelTestFileName);
        }

        CSVFile.toSerial(parallelTestFileName);

        String actualLines = "";
        CSVFile actualFile =
                CSVFile.open(serialTestFileName, CSVFile.READ_MODE);
        String expectedLines = serialMockData;

        String line = "";
        while ((line = actualFile.readLine()) != "")
        {
            actualLines += line + "\n";
        }

        assertEquals(actualLines, expectedLines);
    }
}
