package com.tomst.lolly.ui.graph;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.data.LineDataSet;

import com.github.mikephil.charting.utils.ColorTemplate;
import com.tomst.lolly.core.CSVFile;
import com.tomst.lolly.core.Constants;
import com.tomst.lolly.core.OnProListener;
import com.tomst.lolly.core.TDendroInfo;
import com.tomst.lolly.core.TDeviceType;
import com.tomst.lolly.core.TMereni;
import com.tomst.lolly.core.TPhysValue;
import com.tomst.lolly.databinding.FragmentGraphBinding;
import com.tomst.lolly.core.DmdViewModel;

public class GraphFragment extends Fragment  {
    public int headerIndex = 0;
    public int numDataSets = 0;
    private final int barCount = 12;
    private String CsvFileName;
    private CombinedChart chart;
    private CombinedData combinedData;

    private ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    private ArrayList<TDendroInfo> dendroInfo = new ArrayList<>();

    private SeekBar seekBarX;
    private TextView tvX;

    private final int[] colors = new int[] {
            ColorTemplate.VORDIPLOM_COLORS[0],
            ColorTemplate.VORDIPLOM_COLORS[1],
            ColorTemplate.VORDIPLOM_COLORS[2]
    };

    private FragmentGraphBinding binding;

    private  DmdViewModel dmd;

    private Integer fIdx=0;
    //loading constants
    private static String OFFPATTERN = "dd.MM.yyyy HH:mm";
    private static final byte SERIAL_INDEX = 0;
    private static final byte START_LINE_INDEX = 1;
    private static final byte LONGITUDE_INDEX = 2;
    private static final byte LATITUDE_INDEX = 3;
    private static final byte PICTURE_INDEX = 4;
    private static final byte iT1=3;
    private static final byte iT2=4;
    private static final byte iT3=5;
    private static final byte iHum=6;
    private static final byte iMvs=7;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void LoadCsvFile(String fileName){
        int valueIndex = 0;
        CSVFile csv = CSVFile.open(fileName, CSVFile.READ_MODE);

        //read header of file
        String currentline = "";

        currentline = csv.readLine();
        numDataSets = Integer.parseInt(currentline.split(";")[0]);

        while (headerIndex < numDataSets)               //looping through header
        {
            currentline = csv.readLine();
            String[] lineOfFile = currentline.split(";");
            dendroInfo.get(headerIndex).serial = lineOfFile[SERIAL_INDEX];
            dendroInfo.get(headerIndex).startLine = Long.parseLong(lineOfFile[START_LINE_INDEX]);
            dendroInfo.get(headerIndex).longitude = Long.parseLong(lineOfFile[LONGITUDE_INDEX]);
            dendroInfo.get(headerIndex).latitude = Long.parseLong(lineOfFile[LATITUDE_INDEX]);

            headerIndex++;
        }
        headerIndex=0;
        while ((currentline = csv.readLine()) != "")     //looping through rest of file
        {
            TMereni mer = processLine(currentline);
            dendroInfo.get(headerIndex).mers.add(mer);
            dendroInfo.get(headerIndex).vT1.add(new Entry(valueIndex, (float)mer.t1 ));
            dendroInfo.get(headerIndex).vT2.add(new Entry(valueIndex, (float)mer.t2 ));
            dendroInfo.get(headerIndex).vT3.add(new Entry(valueIndex, (float)mer.t3 ));
            dendroInfo.get(headerIndex).vHA.add(new Entry(valueIndex, (float)mer.hum ));
            valueIndex++;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private TMereni processLine( String line ){
        int currDay = 0;
        LocalDateTime dateTime = null;
        LocalDateTime currDate;

        TMereni mer = new TMereni();

        String[] lineOfFile = line.split(";");

        if (lineOfFile.length == 1)  //requires changing csv to have serial before dataset
        {
            headerIndex++;
        }
        else
        {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(OFFPATTERN);
                dateTime = LocalDateTime.parse(lineOfFile[1], formatter);
                mer.dtm = dateTime;
                mer.day = dateTime.getDayOfMonth();

            } catch (Exception e) {
                System.out.println(e);
            }
            String T1 = lineOfFile[iT1].replace(',', '.');//replaces all occurrences of 'a' to 'e'
            String T2 = lineOfFile[iT2].replace(',', '.');//replaces all occurrences of 'a' to 'e'
            String T3 = lineOfFile[iT3].replace(',', '.');//replaces all occurrences of 'a' to 'e'
            mer.t1 = Float.parseFloat(T1);
            mer.t2 = Float.parseFloat(T2);
            mer.t3 = Float.parseFloat(T3);
            mer.hum = Integer.parseInt(lineOfFile[iHum]);
            mer.mvs = Integer.parseInt(lineOfFile[iMvs]);
        }
        return mer;
    }
    private void DoBtnClick(View view){
        boolean checked = ((CheckBox) view).isChecked();
        Object ob = ((CheckBox) view).getTag();
        int tag = Integer.valueOf(ob.toString());
        if (tag<=0)
            throw new UnsupportedOperationException("Selected linedataset doesnt exists");

        ((LineDataSet) dataSets.get(tag-1)).setVisible(checked);
        chart.invalidate();
    }

    // nahraje data pridane TMD adapterem ve fragmentu HomeFragment
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        GraphViewModel readerViewModel =  new ViewModelProvider(this).get(GraphViewModel.class);
        binding = FragmentGraphBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        dmd = new ViewModelProvider(getActivity()).get(DmdViewModel.class);

        // sem me dostane observer nastaveny v prohlizecce souboru
        dmd.getMessageContainerGraph().observe(getViewLifecycleOwner(), AFileName -> {
            if (AFileName.equals("TMD"))
               DisplayData();  // vytahne data z dmd, ktere sem poslal TMD adapter
            else
               LoadCsvFile(AFileName);

           dmd.getMessageContainerGraph().removeObservers(getViewLifecycleOwner());
        });

        CheckBox cbT1 = binding.vT1;
        cbT1.setChecked(true);
        cbT1.setOnClickListener(view -> {
           DoBtnClick(view);
        });


        CheckBox cbT2 = binding.vT2;
        cbT2.setChecked(true);
        cbT2.setOnClickListener(view -> {
            DoBtnClick(view);
        });

        CheckBox cbT3 = binding.vT3;
        cbT3.setChecked(true);
        cbT3.setOnClickListener(view -> {
            DoBtnClick(view);
        });

        CheckBox cbHum = binding.vHum;
        cbHum.setChecked(true);
        cbHum.setOnClickListener(view -> {
            DoBtnClick(view);
        });


        getActivity().setTitle("Lolly 4");
        chart = binding.chart1;

        //chart.getDescription().setText(CsvFileName);
        chart.setTouchEnabled(true);
        chart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(true);

        // set an alternative background color
       // chart.setBackgroundColor(Color.WHITE);
        chart.setViewPortOffsets(0f, 0f, 0f, 0f);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

         // get the legend (only possible after setting data)
        Legend l = chart.getLegend();
        /*
        l.setWordWrapEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setEnabled(false);
        */

        l.setForm(Legend.LegendForm.LINE);
        //l.setTypeface(tfLight);
        l.setTextSize(11f);
        l.setTextColor(Color.BLACK);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(true);


        // osa humidit
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setDrawGridLines(true);
        rightAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        //rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        //rightAxis.setAxisMaximum(1000f);

        // osa teplot
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        //leftAxis.setAxisMinimum(-10f); // this replaces setStartAtZero(true)
        //leftAxis.setAxisMaximum(30f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour

        combinedData = new CombinedData();

        //chart.invalidate();
        //setRandomData(400,100);
        return root;
    }

    private LineDataSet SetLine(ArrayList<Entry> vT, TPhysValue val){

        //LineData d = new LineData();
        LineDataSet set = new LineDataSet(vT, "DataSet " + (val.ordinal() + 1));

        set.setLineWidth(2f);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setDrawFilled(false);
        set.setLabel(val.valToString(val));


        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        switch (val){
            case vT1:
                //set.setColor(Color.BLUE);
                set.setColor(Color.rgb(68, 102, 163));

                break;
            case vT2:
                //set.setColor(Color.MAGENTA);
                set.setColor(Color.rgb(243, 156, 53));
                break;
            case vT3:
                //set.setColor(Color.GREEN);
                set.setColor(Color.rgb(0, 128, 0));
                break;
            case vHum:
            case vAD:
            case vMicro:
                set.setColor(Color.BLACK);
                set.setColor(Color.rgb(128, 0, 0));
                set.setAxisDependency(YAxis.AxisDependency.RIGHT);
                break;
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }


        return set;
    }

    private void DisplayData(){
        LineDataSet d = null;
        int ogHeaderIndex = headerIndex;
        headerIndex = 0;

        do {
            // line graph
            d = SetLine(dendroInfo.get(headerIndex).vT1, TPhysValue.vT1);
            dataSets.add(d);
            d = SetLine(dendroInfo.get(headerIndex).vT2, TPhysValue.vT2);
            dataSets.add(d);
            d = SetLine(dendroInfo.get(headerIndex).vT3, TPhysValue.vT3);
            dataSets.add(d);
            // humidity
            d = SetLine(dendroInfo.get(headerIndex).vHA, TPhysValue.vHum);
            dataSets.add(d);
            LineData lines = new LineData(dataSets);
            combinedData.setData(lines);

            // combinedData.setData(generateBarData());
            chart.setData(combinedData);
            chart.getAxisLeft().setEnabled(true);
            chart.getAxisRight().setEnabled(true);

            // startup animation
            chart.animateX(2000, Easing.EaseInCubic);

            // sets view to start of graph and zooms into x axis by 7x
            chart.zoomAndCenterAnimated(7f, 1f, 0, 0, chart.getAxisLeft().getAxisDependency(), 3000);

            headerIndex++;
        } while (headerIndex < numDataSets);

        headerIndex = ogHeaderIndex;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //in works
    /*
    protected float getRandom(float range, float start) {
        return (float) (Math.random() * range) + start;
    }

    private BarData generateBarData() {

        ArrayList<BarEntry> entries1 = new ArrayList<>();
        ArrayList<BarEntry> entries2 = new ArrayList<>();

        for (int index = 0; index < barCount; index++) {
            entries1.add(new BarEntry(0, getRandom(25, 25)));

            // stacked
            entries2.add(new BarEntry(0, new float[]{getRandom(13, 12), getRandom(13, 12)}));
        }

        BarDataSet set1 = new BarDataSet(entries1, "Bar 1");
        set1.setColor(Color.rgb(60, 220, 78));
        set1.setValueTextColor(Color.rgb(60, 220, 78));
        set1.setValueTextSize(10f);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);

        BarDataSet set2 = new BarDataSet(entries2, "");
        set2.setStackLabels(new String[]{"Stack 1", "Stack 2"});
        set2.setColors(Color.rgb(61, 165, 255), Color.rgb(23, 197, 255));
        set2.setValueTextColor(Color.rgb(61, 165, 255));
        set2.setValueTextSize(10f);
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);


        float groupSpace = 0.06f;
        float barSpace = 0.02f; // x2 dataset
        float barWidth = 0.45f; // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        BarData d = new BarData(set1, set2);
        d.setBarWidth(barWidth);

        // make this BarData object grouped
        d.groupBars(0, groupSpace, barSpace); // start at x = 0

        return d;
    }
    */
}