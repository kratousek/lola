package com.tomst.lolly.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class TMSReader extends Thread
{
    public final int SPI_DOWNLOAD_NONE = 0;
    public final int SPI_DOWNLOAD_ALL = 1;
    public final int SPI_DOWNLOAD_BOOKMARK = 2;
    public final int SPI_DOWNLOAD_DATE = 3;
    private final int BOOKMARK_DAY_CONVERSION = 848;

    // vystupy ven z tridy
    public int capUsed = 0;  // kapacita v %
    public long delta = 0;  // rozdil mezi casem v telefonu a v TMS
    public Instant phoneTime = null;
    @SuppressLint("NewApi")
    public Instant lollyTime = Instant.MIN;
    public String lollyTimeString = "";
    private Context DeviceUARTContext;
    private D2xxManager ftdid2xx = null;
    private int currentIndex = -1;
    private final int openIndex = 0;
    private boolean uart_configured = true;
    private static Handler progressBarHandler = new Handler(Looper.getMainLooper());  // handler pro vysilani z Threadu
    public OnProListener mBarListener; // listener field
    private int progressBarStatus=0;
    public TMereni mer ;
    public String sMeteo;
    public TMeteo meteo;
    private int fAddr;
    private int TmpNan = -200;
    private final String TAG = "TOMST";
    public String SerialNumber;
    public String AdapterNumber;
    public RFirmware rfir;


    private static volatile TDevState devState;
    public void SetDevState(TDevState devState){
        this.devState = devState;
    }

    public TDevState GetDevState(){
        return devState;
    }

    private String fileDir;

    // handler pro vystup dat ven z tridy
    private static Handler handler = null;
    private static Handler datahandler = null;

    //private static Handler finalhandler = null;
    private static uHer fHer;
    private int DevCount;
    private boolean bReadThreadGoing;
    private boolean mRunning;

    public void SetHer(uHer her){
        this.fHer = her;
    }
    public void SetHandler(Handler han){  this.handler = han; }

    public void SetBarListener(OnProListener AListener){
        this.mBarListener = AListener;
    }
    public void SetDataHandler(Handler han) {this.datahandler= han;}

    public void SetFilePath(String AFileDir){
        this.fileDir = AFileDir;
    }

    // FileName
    //String CsvFileName = "";
    //boolean writeTxf = false;

    // handler k TMD adapteru
    private FT_Device ftDev = null;

    public void SetDevice(FT_Device Dev) {this.ftDev =Dev;}

    private final Context context;

    // chci, aby firmware existoval v danem kontextu
    public TMSReader(Context context){
        this.context = context;
        fHer = null;

        rfir = new RFirmware();
        mer  = new TMereni(); // mereni

        bReadThreadGoing = false;
    }

    /*
    public void setCsvFileName(String AFileName){
        this.CsvFileName = AFileName;

        if (AFileName.contains(".txf")){
            this.writeTxf = false;
            return;
        }
    }
     */

     /*
    private void DoProgress(long pos){
        //progressBarHandler.sendEmptyMessage( (int)pos);
        progressBarHandler.post(new Runnable() {
            public void run() {
                if (mBarListener != null){
                    mBarListener.OnProEvent(pos);
                }
            }
        });
    }
     */

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void DoProgress(int pos){
        Message message = handler.obtainMessage();
        TInfo info = new TInfo();
        info.stat  = TDevState.tProgress;
        info.idx   = pos;
        if ((pos>0) && (pars.lastDateTrace != null)) {
            long days = DaysBetween(pars.lastDateTrace,GetPhoneTime());
            info.msg = String.format("Remain %d days", days);
            info.remainDays = days;
            info.currDay = pars.lastDateTrace;
        }
        message.obj = info;
        handler.sendMessage(message);
    }
    private void SendMeasure(TDevState stat, String msg) { // Handle sending message back to handler
        Message message = handler.obtainMessage();
        TInfo info = new TInfo();
        info.stat  = stat;
        info.msg   = msg;
        message.obj = info;
        handler.sendMessage(message);
    }

    private void SendMex(TDevState stat, TMereni mer){
        Message message = handler.obtainMessage();

        TInfo info = new TInfo();
        info.stat  = stat;
        info.msg   = "measure";
        info.t1 = mer.t1;
        info.t2 = mer.t2;
        info.t3 = mer.t3;
        info.humAd = mer.hum;

        message.obj = info;
        handler.sendMessage(message);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run()
    {
        Looper.prepare();
        mLoop();
    }

    public void SetRunning(boolean running){
        mRunning = running;
    };

    public static String aft(String line, String splitChar){
        if (line.length()<1)
            return "";

        String [] str = line.split(splitChar);
        if (str.length >1)
            return (str[1]);
        return "";
    }

    public static String between(String line, String s1, String s2){
        String subStr = line.substring(line.indexOf(s1 ) + 1, line.indexOf(s2));
        return subStr;
    }

    public boolean ParseHeader(String line)
    {
        // @ =&93^33%01.80 TMS-A   // dendrometr
        // @ =&93^A0%01.80 TMSx1#
        boolean result = false;
        line = aft(line,"=");
        rfir.Result = line.length()>0;
        if (rfir.Result == false)
            return (false);

        String s = between(line,"&","^");
        rfir.Hw =  (byte)Integer.parseInt(s);
        rfir.Striska = between(line,"^","%");

        int i  = line.indexOf("%");
        s = line.substring(i+1,i+3);
        rfir.Fw = (byte)Integer.parseInt(s);

        s = line.substring(i+4,i+6);
        rfir.Sub= (byte)Integer.parseInt(s);

        i = line.indexOf("TMS3");
        if (i>1) {
            rfir.DeviceType = TDeviceType.dLolly3;
            return true;
        }

        // kdyz tu neni TMS, je hlavicka blbe
        i = line.indexOf("TMS");
        if (i <1)
            return false;

        //char c = line.charAt(line.length()-1);
        char c = line.charAt(i+4);
        switch (c){
            case 'T':
                rfir.DeviceType = TDeviceType.dTermoChron;
                break;

            case 'A':
                rfir.DeviceType = TDeviceType.dAD;
                break;

            case '4':
                rfir.DeviceType = TDeviceType.dLolly4;
                break;

            case '2':
                rfir.DeviceType = TDeviceType.dLolly4;
                break;

            case '#':
                rfir.DeviceType = TDeviceType.dLolly4;
                break;

            case '3':
                rfir.DeviceType = TDeviceType.dLolly3;
                break;

            default:
                rfir.DeviceType = TDeviceType.dUnknown;
        }
        return rfir.Result;
    }

    public  void clearMer()
    {
        mer.t1  = 0;
        mer.t2  = 0;
        mer.t3  = 0;
        mer.hum = 0;
        mer.adc = 0;
        // mer.dev = TDeviceType.dUnknown;
        mer.mvs = 0;
        mer.year= 0;
        mer.day = 0;
        mer.mm  = 0;
        mer.hh  = 0;
        mer.ss  = 0;
    }


    public  byte con(String str, byte pos)
    {
        String s = str.substring(pos,pos+1);
        byte r  = (byte)Integer.parseInt(s,16);
        return r;
    }

    public boolean convertMereni(String line)
    {
        Log.d("|||DEBUG|||", "line: " + line);
        String[] str = line.split(";");
        // 00F;ADC;FF3;183;2
        if (str[1].equals("ADC")) {
            byte k = con(str[0], (byte) 0);
            byte l = con(str[0], (byte) 1);
            byte m = con(str[0], (byte) 2);

            byte x = con(str[2], (byte) 0);
            byte y = con(str[2], (byte) 1);
            byte z = con(str[2], (byte) 2);

            if (m != y) {
                mer.Err = 1;
                return false;
            }

            if (z == 3) {
                mer.dev = TDeviceType.dAD;
            } else if (z == 1) {
                mer.dev = TDeviceType.dTermoChron;
            } else {
                mer.Err = 3;
                mer.dev = TDeviceType.dUnknown;
                return false;
            }
        } else {
            mer.dev = TDeviceType.dLolly4;
        }

        /// rozeber zbylou cast podle typu zarizeni
        clearMer();

        mer.hum = Integer.parseInt(str[0],16);
        int tt1 = Integer.parseInt(str[1],16);
        int tt2 = Integer.parseInt(str[2],16);
        int tt3 = Integer.parseInt(str[3],16);

        String s = str[4].replaceAll("(\\r|\\n)", "");
        int mvs = Integer.parseInt(s,16);

        Log.d("|||DEBUG|||", "hum: " + mer.hum);

        if ((mer.dev == TDeviceType.dAD) || (mer.dev == TDeviceType.dAdMicro)) {
//            mer.hum = 0;
            mer.t1  = convertTemp(tt3);
            mer.t2  = TmpNan;
            mer.t3  = TmpNan;

            s = str[0].substring(0,2) + str[2].substring(0,2);
            mer.adc = Integer.parseInt(s,16);
        }
        else if (mer.dev == TDeviceType.dTermoChron){
            mer.t1 = convertTemp(tt3);
        }
        else {
            mer.t1 = convertTemp(tt1);
            mer.t2 = convertTemp(tt2);
            mer.t3 = convertTemp(tt3);
            mer.mvs = mvs ;
        }
        return true;
    }

    public  double con (int b1)
    {
        double result = 0;
        int bx = b1 & 8;

        if ((b1 & 8) >0)
            result = 0.5;

        if ((b1 & 4) >0)
            result = result + (double)1/4;

        if ((b1 & 3) >0)
            result = result + (double)1/8;

        if ((b1 & 1) >0)
            result = result + (double)1/16;

        return (result);
    }


    public int complementTwo(int hb, int lb){
        lb = lb & 0xF0;
        int result = hb * 256 + lb;
        result = 0xFFFF-result;
        result = result +1;

        return result;
    }


    public double convertTemp(int ttx){
        double result = 0;

        ttx    = ttx * 16;
        int hiTemp =  (ttx >> 8);
        int loTemp =  (ttx & 255);

        loTemp = loTemp & 0xF0;
        if ((hiTemp & 0x80)>0){
            int d  = complementTwo(hiTemp,loTemp);
            hiTemp = d >> 8;
            loTemp = d & 0xFF;
            result = hiTemp + con(loTemp);
            result = - result;
        }
        else {
            loTemp = loTemp >> 4;
            result = hiTemp + con(loTemp);
        }

        return (result);
    }

    /*
    private String WaitForTouch(String cmd){
    }
    */

    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)
    {
        if (ftDev == null)
            return;

        if (ftDev.isOpen() == false) {
            Log.e("j2xx", "SetConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // TODO : flow ctrl: XOFF/XOM
        // TODO : flow ctrl: XOFF/XOM
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);

        uart_configured = true;
        // Toast.makeText(DeviceUARTContext, "Config done", Toast.LENGTH_SHORT).show();
    }

    private boolean connectFunction()
    {
        try
        {
            //int tmpProtNumber = openIndex + 1;
            if (currentIndex != openIndex) {
                if (null == ftDev) {
                    ftDev = ftdid2xx.openByIndex(DeviceUARTContext, openIndex);
                } else {
                    synchronized (ftDev) {
                        ftDev = ftdid2xx.openByIndex(DeviceUARTContext, openIndex);
                    }
                }
                uart_configured = false;
            } else {
                // Toast.makeText(DeviceUARTContext, "Device port " + tmpProtNumber + " is already opened", Toast.LENGTH_LONG).show();
                return true;
            }

            if (ftDev == null) {
                //Toast.makeText(DeviceUARTContext, "open device port(" + tmpProtNumber + ") NG, ftDev == null", Toast.LENGTH_LONG).show();
                return false;
            }

            if (true == ftDev.isOpen()) {
                currentIndex = openIndex;
                //Toast.makeText(DeviceUARTContext, "open device port(" + tmpProtNumber + ") OK", Toast.LENGTH_SHORT).show();

                if (false == bReadThreadGoing) {
                    //read_thread = new readThread(handler);
                    fHer = new uHer(handler);
                    fHer.ftDev = ftDev;
                    fHer.bReadThreadGoing = false;

                }
            } else {
                //Toast.makeText(DeviceUARTContext, "open device port(" + tmpProtNumber + ") NG", Toast.LENGTH_LONG).show();
                Toast.makeText(DeviceUARTContext, "Need to get permission!", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (Exception e) {
            //throw new RuntimeException(e);
            Log.e(TAG,e.getMessage());
            Toast.makeText(DeviceUARTContext, "No FTDI device found !", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean DoInitFTDI(Context mContext){
        try {
            ftdid2xx = D2xxManager.getInstance(mContext);
        } catch (D2xxManager.D2xxException ex) {
            ex.printStackTrace();
            return(false);
        }

        if(!ftdid2xx.setVIDPID(0x0403, 0xada1))
            Log.i("ftd2xx-java","setVIDPID Error");

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);

        DeviceUARTContext = mContext;
        int tempDevCount = ftdid2xx.createDeviceInfoList(DeviceUARTContext);
        return(true);
    }

    // gets number of devices
    public void createDeviceList()
    {
        //DeviceUARTContext = getContext();
        if (context == null) {
            Toast.makeText(DeviceUARTContext, "No valid app context, please check your constructor !", Toast.LENGTH_SHORT).show();
            return;
        }
        DeviceUARTContext = this.context;

        // kolik jsem nasel adapteru na USB hubu ?
        int tempDevCount = ftdid2xx.createDeviceInfoList(DeviceUARTContext);
        if (tempDevCount > 0)
        {
            if( DevCount != tempDevCount )
            {
                DevCount = tempDevCount;
                //updatePortNumberSelector();
            }
        }
        else
        {
            DevCount = -1;
            currentIndex = -1;
        }
    }

    public void ConnectDevice(){
        DevCount = 0;
        DoInitFTDI(context);
        createDeviceList();
        if(DevCount > 0)
        {
            connectFunction();
            int baudRate = 500000;
            byte dataBit = 8;
            byte parity = 0;
            byte stopBit =0;
            byte flowControl = 0;

            SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
        }
    }

    private int copyByte(String line,int i, int count)
    {
        int hi = Integer.parseInt(line.substring(i-1,i-1+count));
        return (hi);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void mLoop()
    {
        String s = "";
        TInfo info;
        info = new TInfo();

        devState = TDevState.tStart;
        lollyTime = Instant.MIN;

        if (fHer==null)
        {
            //throw new UnsupportedOperationException("Communication is not ready");
            SendMeasure(TDevState.tNoHardware, "fHer==null !");
            return;
        }

        while (( devState != TDevState.tFinal )
                && (devState != TDevState.tError)
                && (mRunning==true))
        {
            // devState
            Log.e(TAG, devState.toString());

            switch (devState)
            {
                case tStart:
                    SendMeasure(TDevState.tStart, "test");
                    if (startFTDI())
                        devState = TDevState.tWaitForAdapter;
                    else
                        SystemClock.sleep(1000);
                    break;

                case tWaitForAdapter:
                    AdapterNumber = fHer.getAdapter();
                    SendMeasure(TDevState.tWaitForAdapter, AdapterNumber);

                    if (AdapterNumber.length()>5)
                        devState = TDevState.tHead;
                    else
                        devState = TDevState.tFinal;  // tady by melo byt, ze jsem nenasel adapter
                    break;

                case tFirmware:
                    break;

                case tSerialDuplicity:
                    s = fHer.doCommand("#");
                    if (s.length()<3) {
                        devState = TDevState.tFinal;
                        break;
                    }
                    // seriove cislo
                    s = aft(s, "=");

                    // duplicita
                    if (s.compareToIgnoreCase(SerialNumber) > 0)
                    {
                        SendMeasure(TDevState.tSerialDuplicity,"s");
                        break;
                    }
                    devState = TDevState.tHead;
                    break;

                case tHead:
                    s = fHer.doCommand(" ");
                    // rozeber hlavicku a nastav rfir

                    if (s.length()<2)
                        break;

                    if (ParseHeader(s)) {
                        info.msg = String.format("%d.%d.%d",rfir.Hw,rfir.Fw,rfir.Sub);
                        SendMeasure(TDevState.tHead,s);
                        devState = TDevState.tSerial;
                    }
                    else {
                        //devState = TDevState.tFinal;
                        Log.e(TAG,"Wrong Header:!"+s+" reading thread killed !");
                    }
                    break;

                case tSerial:
                    s = fHer.doCommand("#");
                    if (s.length()<3) {
                        devState = TDevState.tFinal;
                        break;
                    }
                    SerialNumber = aft(s,"=");
                    SerialNumber = SerialNumber.replaceAll("(\\r|\\n)", "");
                    mer.Serial = SerialNumber;
                    SendMeasure(TDevState.tSerial,SerialNumber);
                    devState = TDevState.tInfo;
                    break;


                case tInfo:
                    s = fHer.doCommand("W");
                    if (s.indexOf("@W.") ==-1)  // pockej az se domeri
                        break;

                    SystemClock.sleep(600);
                    s = fHer.doCommand("Q");
                    s = aft(s,"=");
                    if (s.length()<3) {
                        //devState = TDevState.tFinal; // chyba -> konec
                        Log.e(TAG,"tInfo: Not answering to Q: "+s);
                        break;
                    }
                    // ted bych tu mel mit neco jako
                    // '00F;ADC;FF3;1A7;2'  -> 'Tx=26.4, ADC=255'
                    if (convertMereni(s) == true)
                    {
                        // nakompiluj vysledky mereni do stringu a odesli
                        // info.t1 = mer.t1;
                        // info.t2 = mer.t2;
                        // info.t3 = mer.t3;

                        SendMex(TDevState.tInfo,mer);
                    }
                    //devState = TDevState.tGetTime;

                    // get option for showing graph
                    boolean showMicro = context.getSharedPreferences(
                            "save_options", Context.MODE_PRIVATE
                    ).getBoolean("showmicro", false);

                    // if setting for setupAD is no, move on to capacity
                    if (!showMicro) {
                        devState = TDevState.tCapacity;
                    }
                    break;

                case tCapacity:
                    s = fHer.doCommand("P");
                    if (s.length() <8)
                        continue;

                    s = aft(s,"=");

                    int hi = shared.CopyHex(s,2,2);
                    int me = shared.CopyHex(s,4,2);
                    int lo = shared.CopyHex(s,6,2);

                    int lastAddress = hi*256*256+me*256+lo;
                    float d = Float.valueOf(lastAddress) / Float.valueOf(0x3FFFFF) * 100;
                    capUsed  = Math.round(d);
                    mer.msg = String.valueOf(capUsed);
                    SendMeasure(TDevState.tCapacity, mer.msg);
                    devState = TDevState.tGetTime;

                    break;

                case tGetTime:
                    s = fHer.doCommand("C");
                    lollyTime = parseDateTime(s);   // dostanu utc cas + zonu, bez letniho casu
                    mer.msg = lollyTimeString;      // lollyTimeString nastavuju v parseDateTime
                    SendMeasure(TDevState.tGetTime, mer.msg);

                    devState = TDevState.tCompareTime;
                    break;

                case tCompareTime:
                    // srovnej cas z lizatka s casem zarizeni
                    phoneTime = GetPhoneTime();

                    long eLolly = lollyTime.toEpochMilli();
                    long ePhone = phoneTime.toEpochMilli();
                    delta  = Math.abs(ePhone - eLolly);
                    if (delta>5*1000)
                        devState = TDevState.tSetTime;
                    else
                        devState = TDevState.tReadMeteo;

                    mer.msg = String.valueOf(delta);
                    SendMeasure(TDevState.tCompareTime,mer.msg);
                    break;

                case tSetTime:
                    s = getDateTime();
                    s = "C="+s;
                    s = fHer.doCommand(s);
                    s = fHer.doCommand("C");

                    devState = TDevState.tReadMeteo;
                    break;

                case tReadMeteo:
                    s = fHer.doCommand("M");
                    meteo = getMeteo(s);
                    sMeteo  = shared.MeteoToString(meteo);
                    info.meteo = meteo;

                    // odesli meteo
                    Message message = handler.obtainMessage();
                    TInfo inx = new TInfo();
                    inx.stat  = devState;
                    inx.msg   = sMeteo;
                    message.obj = inx;
                    handler.sendMessage(message);

                    SendMeasure(TDevState.tReadMeteo,sMeteo);

                    devState = TDevState.tSetMeteo;
                    break;

                case tSetMeteo:
                    // nastavovat meteo muzu, az bude formular s nastavenim
                    devState = TDevState.tReadData;
                    break;

                case tReadData:
                    devState = TDevState.tCheckTMSFirmware; // nekonecna smycka, SMAZAT !!!!

                    if (!ReadData()){
                        devState = TDevState.tError;
                        break;
                    }
                    devState = TDevState.tFinishedData;
                    break;

                case tFinishedData:
                    SendMeasure(TDevState.tFinishedData,"FINISHED!");
                    devState = TDevState.tWaitInLimbo;

                    // tell the user it has downloaded, and play a sound
                    Toast.makeText(this.context, "Finished! Remove the device!", Toast.LENGTH_SHORT).show();
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone ringtone = RingtoneManager.getRingtone(context, notification);
                    ringtone.play();

                    break;

                case tWaitInLimbo:
                    try {
                        Thread.sleep(5000); // 5 seconds
                        devState = TDevState.tStart;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    break;

                case tCheckTMSFirmware:
                    break;
            }
        }

        Log.e("TOMST", devState.toString());
    }

    private boolean startFTDI(){
        if (ftDev == null)
        {
            Log.e("j2xx","SendMessage : d2xx device doesnt exist");
            //LogMsg("Device doesnt exist. Check USB port");
            return false;
        }

        if (ftDev.isOpen() == false) {
            Log.e("j2xx", "SendMessage: device not open");
            return false;
        }

        if (fHer == null)
            throw new UnsupportedOperationException("startFTDI.fher is null !");

        fHer.prepcom(); //rts/dtr on/off
        byte[] b = new byte[5];

        String adapter = fHer.getAdapter();
        if (adapter.isEmpty()) {
            //LogMsg("No adapter present");
            Log.e("j2xx","SendMessage: cannot read adapter number");
            return false;
        }

        return true;
    }

    private int getaddr(String respond)
    {
        //String s = new String("P=$05ED00");
        try {
            String[] arr = respond.split("=", 2);
            String s = arr[1].substring(1);
            s = s.replaceAll("(\\r|\\n)", "");
            int ret = Integer.parseInt(s, 16);
            return (ret);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    private String getHexValueFromBookmark(int pValue, int bookmarkDays) {
        // subtract p value (current pointer) from days converted to int/hex
        int finalValue = pValue - (bookmarkDays * BOOKMARK_DAY_CONVERSION);
        finalValue = Math.max(0, finalValue); // clamp to 0
        String hexString = Integer.toHexString(finalValue).toUpperCase();

        // pad the beginning of the string with 0's if less than 6 characters
        StringBuilder finalHexStringBuilder = new StringBuilder();
        for (int i = 0; i < 6 - hexString.length(); i++) {
            finalHexStringBuilder.append("0");
        }
        finalHexStringBuilder.append(hexString);
        String finalHexString = finalHexStringBuilder.toString();

        Log.d("Sendmessage", "Bookmark hex value: " + finalHexString);

        return finalHexString;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean ReadData()
    {
        String respond = "";
        pars parser = new pars();
        pars.SetHandler(datahandler);
        pars.SetDeviceType(rfir.DeviceType);

        SharedPreferences prefs = context.getSharedPreferences(
                "save_options", Context.MODE_PRIVATE
        );
        int readFromSpinnerIndex = prefs.getInt("readFrom", SPI_DOWNLOAD_NONE);
        int bookmarkDays = prefs.getInt("bookmarkVal", 0);
        String readFromDate = prefs.getString("fromDate", "");

        if (ftDev == null)
        {
            Log.e("j2xx","SendMessage : d2xx device doesnt exist");
            //LogMsg("Device doesnt exist. Check USB port");
            return false;
        }

        if (!ftDev.isOpen())
        {
            Log.e("j2xx", "SendMessage: device not open");
            return false;
        }

        // napocitej pocet cyklu z posledni adresy
        String pRespond = fHer.doCommand("P");
        int lastAddress = getaddr(pRespond);
        DoProgress(-lastAddress);  // celkovy pocet bytu

        switch (readFromSpinnerIndex) {
            case SPI_DOWNLOAD_NONE:
            case SPI_DOWNLOAD_ALL:
                respond = fHer.doCommand("S=$000000");
                Log.d("Sendmessage", respond);
                break;
            case SPI_DOWNLOAD_BOOKMARK:
                // find hex value for bookmark based on days value
                String bHexString = getHexValueFromBookmark(lastAddress, bookmarkDays);

                // do B command to set bookmark
                respond = fHer.doCommand("B=$" + bHexString);
                Log.d(TAG, respond);

                // do S command with B value
                respond = fHer.doCommand("S=$" + bHexString);
                Log.d("SendMessage", respond);

                break;
            case SPI_DOWNLOAD_DATE:
                int daysBetween = 0;
                if (!readFromDate.isEmpty()) {
                    // find how many days from now to the read from date is
                    LocalDate fromDate = LocalDate.parse(readFromDate);
                    LocalDate todaysDate = LocalDate.now();

                    daysBetween = (int) Math.abs(ChronoUnit.DAYS.between(fromDate, todaysDate));
                }

                // find hex value based on days value
                String sHexString = getHexValueFromBookmark(lastAddress, daysBetween);

                // do S command with hex value
                respond = fHer.doCommand("S=$" + sHexString);
                Log.d("SendMessage", respond);
        }

        fAddr = 0;

        String ss = "";
        while ((fAddr < lastAddress) && (mRunning==true))
        {
            // performing operation

            respond = fHer.doCommand("D");
            if (respond.length()>1) {
                ss = parser.dpacket(respond);
                Log.i(TAG,respond);
            };

            // jakou mam aktualne adresu
            respond = fHer.doCommand("S");

            if (respond.length()>1)
                fAddr = getaddr(respond);

            // Updating the progress bar

            DoProgress(fAddr);
        }

        return true;
    }

   /*
    private String MeteoToString(TMeteo met){
        String ret = met.toString();
        if (ret.length()>0)
            ret = ret.substring(1);

        return ret;
    }
    */

    private TMeteo getMeteo(String line){
        if (line.length() <1 )
        {
            Log.e(Constants.TAG,String.format("getMeteo(line) input %s invalid",line));
            return TMeteo.mNone;
        }
        String s = line.replaceAll("(\\r|\\n)", "");
        s = aft(s,"=");
        int idx = Integer.parseInt(s);
        TMeteo met = TMeteo.mNone;

        //Basic 1
        //Meteo 2
        //Smart 3
        //Intensive 4
        //Experiment 8

        switch (idx){
            case 1:
                met = TMeteo.mBasic;
                break;
            case 2:
                met = TMeteo.mMeteo;
                break;
            case 3:
                met = TMeteo.mSmart;
                break;
            case 4:
                met = TMeteo.mIntensive;
                break;
            case 8:
                met = TMeteo.mExperiment;
                break;
            default:
                met = TMeteo.mNone;
        }
        return met;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getDateTime(){
        ZoneId tz = ZoneId.systemDefault();
        String offsetId = tz.getRules().getStandardOffset(Instant.now()).getId();
        int sec = tz.getRules().getStandardOffset(Instant.now()).getTotalSeconds();

        // vypis offset
        String str[] = offsetId.split(":");
        //System.out.println(str[0]);
        //System.out.println(str[1]);
        //System.out.print(sec);

        // offset beru ze systemu
        // = 7200+960; // 2 hod + 15 min
        int hod = (sec / 3600)  ;
        int min = (sec % 3600) / 60; // tady by mely byt minuty
        int gmt= 4*hod + min/15;
        String gmts = String.valueOf(gmt);
        if (gmts.length()==1){
            gmts = "0"+gmts;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd,HH:mm:ss").withZone(ZoneId.of("UTC"));
        final ZonedDateTime jobStartDateTimeZ = ZonedDateTime.now();
        Instant startTime = jobStartDateTimeZ.toInstant();
        String command =  formatter.format(startTime) + "+"+gmts;;
        // System.out.println(command);

        return command;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public long DaysBetween(Instant phoneTime, Instant deviceTime){
        long ret = ChronoUnit.DAYS.between(phoneTime, deviceTime);
        return(ret);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Instant GetPhoneTime(){
        ZoneId tz = ZoneId.systemDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd,HH:mm:ss").withZone(ZoneId.of("UTC"));
        ZonedDateTime jobStartDateTimeZ = ZonedDateTime.now(); // lokalni cas v zone
        Instant startTime = jobStartDateTimeZ.toInstant();     // UTC
        return(startTime);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean TimeIsWrong(Instant lolTime, Instant phoneTime){
        long eLolly = lolTime.toEpochMilli();
        long ePhone = phoneTime.toEpochMilli();
        delta  = Math.abs(ePhone - eLolly);
        return (delta>5*1000);
    }

    // cas ziskany ze zarizeni prevedu na Instant
    // cas prevedu na zonedDateTime, abych mohl porovnavat s casem ziskanym z PC
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Instant parseDateTime(String line){
        // @C=2023/03/30,16:28:52+04
        // @C=2023/03/31,08:32:35+04

        // must check for both + and -, both sides of UTC
        String date = between(line, "=", line.contains("+") ? "+" : "-");
        String zdt = aft(line, line.contains("+") ? "\\+" : "\\-");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd,HH:mm:ss");
        zdt = zdt.replaceAll("(\\r|\\n)", "");
        int sec = Integer.parseInt(zdt) * 900;  // 3600 / 4
        ZoneOffset zx = ZoneOffset.ofTotalSeconds(sec);
        String zof = zx.toString();
        // ZonedDateTime zonedDateTime = ZonedDateTime.parse(date , formatter.withZone(ZoneOffset.ofTotalSeconds(sec)));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(date , formatter.withZone(ZoneOffset.UTC)); // String -> ZoneDateTime v UTC
        Instant startTime = zonedDateTime.toInstant();

        lollyTimeString = zonedDateTime.format(DateTimeFormatter.ofPattern(Constants.DEVICE_FORMAT))+" "+zof+")";

        return startTime;
    }



}
