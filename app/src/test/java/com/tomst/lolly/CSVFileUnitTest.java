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

    private final String parallelTestFileName = "./test_parallel.csv";
    private final String serialTestFileName = "./test.csv";
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
        + "7832;10421;79823;\n"
        + "1;2023.05.01 00:15;0;7,123;-200;-200;937;206;0;"
        + "1;2023.05.01 00:30;0;1,234;-200;-200;563;234;0;"
        + "1;2023.05.01 00:30;0;34,028;-200;-200;120;789;0;\n";

    @Before
    public void setup()
    {
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
        CSVFile.toParallel(serialTestFileName);

        String actualLines = "";
        CSVFile actualFile =
                CSVFile.open(serialTestFileName, CSVFile.READ_MODE);
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
        CSVFile.toSerial(parallelTestFileName);

        String actualLines = "";
        CSVFile actualFile =
                CSVFile.open(parallelTestFileName, CSVFile.READ_MODE);
        String expectedLines = serialTestFileName;

        String line = "";
        while ((line = actualFile.readLine()) != "")
        {
            actualLines += line + "\n";
        }

        assertEquals(actualLines, expectedLines);
    }
}
