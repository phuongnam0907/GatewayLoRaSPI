package phuongnam.gatewayloraspi;

import android.app.Activity;
import android.app.TaskStackBuilder;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Thread.yield;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private SpiDevice mDevice;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Gpio pinReset;  //BCM17 - pin 11
    private Gpio pinD0;     //BCM4  - pin 7
    private Gpio pinCSS;    //BCM25 - pin 22

    int counter = 0;
    private int _ss;
    private int _reset;
    private int _dio0;

    private long _frequency;
    private int _packetIndex;
    private int _implicitHeaderMode;

    /**
    * Public variable in this MainActivity.java
    */
    Timer newLoop;
    int packetLength = 0;


    public static final byte REG_FIFO                   = (byte) 0x00;
    public static final byte REG_OP_MODE                = (byte) 0x01;
    public static final byte REG_FRF_MSB                = (byte) 0x06;
    public static final byte REG_FRF_MID                = (byte) 0x07;
    public static final byte REG_FRF_LSB                = (byte) 0x08;
    public static final byte REG_PA_CONFIG              = (byte) 0x09;
    public static final byte REG_OCP                    = (byte) 0x0b;
    public static final byte REG_LNA                    = (byte) 0x0c;
    public static final byte REG_FIFO_ADDR_PTR          = (byte) 0x0d;
    public static final byte REG_FIFO_TX_BASE_ADDR      = (byte) 0x0e;
    public static final byte REG_FIFO_RX_BASE_ADDR      = (byte) 0x0f;
    public static final byte REG_FIFO_RX_CURRENT_ADDR   = (byte) 0x10;
    public static final byte REG_IRQ_FLAGS              = (byte) 0x12;
    public static final byte REG_RX_NB_BYTES            = (byte) 0x13;
    public static final byte REG_PKT_SNR_VALUE          = (byte) 0x19;
    public static final byte REG_PKT_RSSI_VALUE         = (byte) 0x1a;
    public static final byte REG_MODEM_CONFIG_1         = (byte) 0x1d;
    public static final byte REG_MODEM_CONFIG_2         = (byte) 0x1e;
    public static final byte REG_PREAMBLE_MSB           = (byte) 0x20;
    public static final byte REG_PREAMBLE_LSB           = (byte) 0x21;
    public static final byte REG_PAYLOAD_LENGTH         = (byte) 0x22;
    public static final byte REG_MODEM_CONFIG_3         = (byte) 0x26;
    public static final byte REG_FREQ_ERROR_MSB         = (byte) 0x28;
    public static final byte REG_FREQ_ERROR_MID         = (byte) 0x29;
    public static final byte REG_FREQ_ERROR_LSB         = (byte) 0x2a;
    public static final byte REG_RSSI_WIDEBAND          = (byte) 0x2c;
    public static final byte REG_DETECTION_OPTIMIZE     = (byte) 0x31;
    public static final byte REG_INVERTIQ               = (byte) 0x33;
    public static final byte REG_DETECTION_THRESHOLD    = (byte) 0x37;
    public static final byte REG_SYNC_WORD              = (byte) 0x39;
    public static final byte REG_INVERTIQ2              = (byte) 0x3b;
    public static final byte REG_DIO_MAPPING_1          = (byte) 0x40;
    public static final byte REG_VERSION                = (byte) 0x42;
    public static final byte REG_PA_DAC                 = (byte) 0x4d;

    public static final byte MODE_LONG_RANGE_MODE       = (byte) 0x80;
    public static final byte MODE_SLEEP                 = (byte) 0x00;
    public static final byte MODE_STDBY                 = (byte) 0x01;
    public static final byte MODE_TX                    = (byte) 0x03;
    public static final byte MODE_RX_CONTINUOUS         = (byte) 0x05;
    public static final byte MODE_RX_SINGLE             = (byte) 0x06;
    public static final byte PA_BOOST                   = (byte) 0x80;

    // IRQ masks
    public static final byte IRQ_TX_DONE_MASK           = (byte) 0x08;
    public static final byte IRQ_PAYLOAD_CRC_ERROR_MASK = (byte) 0x20;
    public static final byte IRQ_RX_DONE_MASK           = (byte) 0x40;

    public static final int MAX_PKT_LENGTH              = 255;

    public static final int PA_OUTPUT_RFO_PIN          = 0;
    public static final int PA_OUTPUT_PA_BOOST_PIN     = 1;

    private static final boolean LOW = false;
    private static final boolean HIGH = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PeripheralManager manager = PeripheralManager.getInstance();
        Log.d(TAG,"List of Devices support SPI : "+ manager.getSpiBusList());
        try {
            pinReset = manager.openGpio("BCM17");
            pinD0 = manager.openGpio("BCM4");
            pinCSS = manager.openGpio("BCM25");
            Log.d(TAG,"Name: " + pinReset.getName());
            Log.d(TAG,"Name: " + pinD0.getName());
            Log.d(TAG,"Name: " + pinCSS.getName());

            pinReset.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            pinD0.setDirection(Gpio.DIRECTION_IN);
            pinD0.setActiveType(Gpio.ACTIVE_HIGH);
            pinD0.setEdgeTriggerType(Gpio.EDGE_RISING);
            pinCSS.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);

            mDevice = manager.openSpiDevice("SPI0.1");
            Log.d(TAG,"Name: " + mDevice.getName());

            //Start LoRa
            setup();
            loop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (pinReset != null) {
            try {
                pinReset.close();
                pinReset = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close GPIO", e);
            }
        }

        if (pinD0 != null) {
            try {
                pinD0.close();
                pinD0 = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close GPIO", e);
            }
        }

        if (pinCSS != null) {
            try {
                pinCSS.close();
                pinCSS = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close GPIO", e);
            }
        }

        if (mDevice != null) {
            try {
                mDevice.close();
                mDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close SPI device", e);
            }
        }

    }

    /***************************************************************
     *
     *                      VOID SETUP - START LORA
     *
     ***************************************************************/

    public void setup(){
        try {
            int status = begin(434000000);
            delay(1000);
            if (status == 0) Log.d("LoRa","Init Failed!");
            else Log.d("LoRa","Init Succeed!!! Starting LoRa......");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //printRegisters();
    }

    /***************************************************************
     *
     *                       VOID LOOP
     *
     ***************************************************************/

    public void loop(){
        newLoop = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        LoRaReceive();
                        byte[] data = new byte[MAX_PKT_LENGTH];
                        data = recvByte();
                        int len = getLength();
                        parseJSON(data[0], data[1], data[2], data[3], data[4], data[5]);
                        delay(3000);
//                        counter++;
//                        LoRaSender("phuongnam0907@gmail.com/?value:= " + counter);
//                        int size = parsePacket(0);
//                        String s = new String(String.valueOf((char)readRegister(REG_FIFO)));
//                        Log.d("FIFO: ", s + " IRQ: 0x"+ Integer.toHexString(readRegister(REG_IRQ_FLAGS)) + " size: " + Integer.toString(readRegister(REG_RX_NB_BYTES)) + " payload: " + Integer.toString(readRegister(REG_PAYLOAD_LENGTH)));
//                        writeRegister(REG_FIFO,(byte)0);
//                        Log.d("Init: ",Integer.toHexString(readRegister(REG_VERSION)));
                    }
                });
            }
        };
        newLoop.schedule(timerTask,1000,50);
    }

    /***************************************************************
     *
     *                PARSE AND UPDATE DATA ON SERVER
     *
     ***************************************************************/

    @Override
    protected void onStart() {
        super.onStart();
        // Begin listening for interrupt events
        updatetimer();
    }

    private String header_1 ="[{\"gateway\":\"1\",\"sensor\":[";
    private String result = header_1;
    Timer updateTimer;
    Timestamp timestamp;

    public void parseJSON(byte gatewayid, byte id, byte hum, byte humf, byte tem, byte temf){

    }

    public void updatetimer(){
        updateTimer = new Timer();
        TimerTask update = new TimerTask() {
            @Override
            public void run() {

                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateData();
                    }
                });
            }
        };
        updateTimer.schedule(update,10000,15000);
    }

    private void updateData(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    Date date = new Date();
                    result = result.substring(0,result.length()-1);
                    result += "],\"time\":" + date.getTime()/1000 + "}]";

                    URL url = new URL("http://192.168.0.10:80/backend/post.php");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());

                    os.writeBytes(result.toString());
                    Log.d("json: ", result);
                    os.flush();
                    os.close();

                    BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuffer sb = new StringBuffer("");
                    String line="";

                    while((line = in.readLine()) != null) {

                        sb.append(line);
                        break;
                    }

                    in.close();
                    Log.d("phuongnam0907 response",sb.toString());

                    conn.disconnect();

                    result = "";
                    result = header_1;

                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    /***************************************************************
     *
     *                      CONFIG SPI COMMUNICATE
     *
     ***************************************************************/

    private void configSPIDevice(SpiDevice device) throws IOException {
        device.setMode(SpiDevice.MODE1);
        device.setFrequency(32000000); // 32MHz
        device.setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST);
        device.setBitsPerWord(8);
        Log.d(TAG,"SPI OK now ....");
    }

    /***************************************************************
     *
     *                        SUB-FUNCTION CODE
     *
     ***************************************************************/

    public void LoRaSender(String string){
        beginPacket(0);
        write(string.getBytes());
        endPacket(false);
        delay(5000);
    }

    public void LoRaReceive(){
        int packetSize = parsePacket(0);
        if (packetSize > 0) {
            String dataTemp = "";
            // received a packet
            dataTemp += "Received packet '";
            // read packet
            while (available() > 0) {
                dataTemp += (char)read();
            }

            // print RSSI of packet
            dataTemp += "' with RSSI " + packetRssi();

            Log.d(TAG,dataTemp);
        }
    }

    public byte[] recvByte(){
        int packetSize = parsePacket(0);
        packetLength = 0;
        byte[] dataTemp = new byte[MAX_PKT_LENGTH];
        if (packetSize > 0) {
            // read packet
            int i = 0;
            while (available() > 0) {
                dataTemp[i] = (byte) read();
                i++;
            }
            packetLength = i;
        }
        return dataTemp;
    }

    public int getLength(){
        return packetLength;
    }

    /***************************************************************
     *
     *                           LIBRARY CODE
     *
     ***************************************************************/

    public int begin(long frequency) throws IOException {

        // perform reset
        digitalWrite(pinReset, LOW);
        delay(10);
        digitalWrite(pinReset, HIGH);
        delay(10);


        // start SPI
        configSPIDevice(mDevice);

        // check version
        byte version = readRegister(REG_VERSION);
        if (version != 0x12) {
            return 0;
        }

        // put in sleep mode
        sleep();
/*

        //Test Register
        writeRegister(REG_FIFO_TX_BASE_ADDR, (byte) 0xab);
        Log.d(TAG,"Test: " + Integer.toHexString(readRegister(REG_FIFO_TX_BASE_ADDR)& 0x000000FF));
        writeRegister(REG_FIFO_RX_BASE_ADDR, (byte) 0x89);
        Log.d(TAG,"Test: " + Integer.toHexString(readRegister(REG_FIFO_RX_BASE_ADDR)& 0x000000FF));
        writeRegister(REG_FIFO_TX_BASE_ADDR, (byte) 0x69);
        Log.d(TAG,"Test: " + Integer.toHexString(readRegister(REG_FIFO_TX_BASE_ADDR)& 0x000000FF));
*/

        // set frequency
        setFrequency(434000000);

        // set base addresses
        writeRegister(REG_FIFO_TX_BASE_ADDR, (byte) 0);
        writeRegister(REG_FIFO_RX_BASE_ADDR, (byte) 0);

        // set LNA boost
        writeRegister(REG_LNA, (byte) (readRegister(REG_LNA) | 0x03));

        // set auto AGC
        writeRegister(REG_MODEM_CONFIG_3, (byte) 0x04);

        // set output power to 17 dBm
        setTxPower(13, PA_OUTPUT_PA_BOOST_PIN);

        // put in standby mode
        idle();

        return 1;
    }

    public void end(){
        onDestroy();
    }

    public void digitalWrite(Gpio gpio, boolean value){
        try {
            gpio.setValue(value);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void delay(long micro){
        try {
            Thread.sleep(micro);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void writeRegister(byte address, byte value) {
        singleTransfer((byte) (address | 0x80), value);
    }

    private byte readRegister(byte address) {
        return singleTransfer((byte) (address & 0x7f), (byte) 0x00);
    }

    private byte singleTransfer(byte address, byte value) {
        byte[] respone = new byte[2];
        byte[] data = new byte[2];
        data[0] = address;
        data[1] = value;
        try {
            pinCSS.setValue(false);
            mDevice.transfer(data,respone,data.length);
            pinCSS.setValue(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return respone[1];
    }

    public void setFrequency(long frequency) {
        _frequency = frequency;

        long frf = (frequency << 19) / 32000000;

        writeRegister(REG_FRF_MSB, (byte) (frf >> 16));
        writeRegister(REG_FRF_MID, (byte) (frf >> 8));
        writeRegister(REG_FRF_LSB, (byte) (frf >> 0));
    }

    public void setTxPower(int level, int outputPin) {
        if (PA_OUTPUT_RFO_PIN == outputPin) {
            // RFO
            if (level < 0) {
                level = 0;
            } else if (level > 14) {
                level = 14;
            }

            writeRegister(REG_PA_CONFIG, (byte) (0x70 | level));
        } else {
            // PA BOOST
            if (level > 17) {
                if (level > 20) {
                    level = 20;
                }

                // subtract 3 from level, so 18 - 20 maps to 15 - 17
                level -= 3;

                // High Power +20 dBm Operation (Semtech SX1276/77/78/79 5.4.3.)
                writeRegister(REG_PA_DAC, (byte) 0x87);
                setOCP((byte) 140);
            } else {
                if (level < 2) {
                    level = 2;
                }
                //Default value PA_HF/LF or +17dBm
                writeRegister(REG_PA_DAC, (byte) 0x84);
                setOCP((byte) 100);
            }

            writeRegister(REG_PA_CONFIG, (byte) (PA_BOOST | (level - 2)));
        }
    }

    public void idle(){
        writeRegister(REG_OP_MODE, (byte) (MODE_LONG_RANGE_MODE | MODE_STDBY));
        // Log.d(TAG,"MODE 0x01: 0x" + Integer.toHexString(readRegister(REG_OP_MODE)));
    }

    public void sleep(){
        writeRegister(REG_OP_MODE, (byte) (MODE_LONG_RANGE_MODE | MODE_SLEEP));
        //Log.d(TAG,"MODE 0x01: 0x" + Integer.toHexString(readRegister(REG_OP_MODE)));
    }

    public void printRegisters()
    {
        for (int i = 0; i < 128; i++) {
            Log.d(TAG,"0x"+ Integer.toHexString(i) +": 0x" + Integer.toHexString(readRegister((byte) i)& 0x000000FF));
        }
    }

    public void setOCP(byte mA)
    {
        byte ocpTrim = 27;

        if (mA <= 120) {
            ocpTrim = (byte) ((mA - 45) / 5);
        } else if (mA <=240) {
            ocpTrim = (byte) ((mA + 30) / 10);
        }

        writeRegister(REG_OCP, (byte) (0x20 | (0x1F & ocpTrim)));
    }

    /***************************************************************
     *
     *                           NEW CODE
     *
     ***************************************************************/

    public int beginPacket(int implicitHeader)
    {
        if (isTransmitting()) {
            return 0;
        }

        // put in standby mode
        idle();

        if (implicitHeader > 0) {
            implicitHeaderMode();
        } else {
            explicitHeaderMode();
        }

        // reset FIFO address and paload length
        writeRegister(REG_FIFO_ADDR_PTR, (byte) 0);
        writeRegister(REG_PAYLOAD_LENGTH, (byte) 0);

        return 1;
    }

    public int endPacket(boolean async)
    {
        // put in TX mode
        writeRegister(REG_OP_MODE, (byte) (MODE_LONG_RANGE_MODE | MODE_TX));

        if (async) {
            // grace time is required for the radio
            delay(150);
        } else {
            // wait for TX done
            while ((readRegister(REG_IRQ_FLAGS) & IRQ_TX_DONE_MASK) == 0) {
                yield();
            }
            // clear IRQ's
            writeRegister(REG_IRQ_FLAGS, IRQ_TX_DONE_MASK);
        }

        return 1;
    }

    private boolean isTransmitting()
    {
        if ((readRegister(REG_OP_MODE) & MODE_TX) == MODE_TX) {
            return true;
        }

        if ((readRegister(REG_IRQ_FLAGS) & IRQ_TX_DONE_MASK) > 0) {
            // clear IRQ's
            writeRegister(REG_IRQ_FLAGS, IRQ_TX_DONE_MASK);
        }

        return false;
    }

    public int parsePacket(int size)
    {
        writeRegister(REG_OP_MODE, (byte) (MODE_LONG_RANGE_MODE | MODE_RX_SINGLE));
        int packetLength = 0;
        int irqFlags = readRegister(REG_IRQ_FLAGS);

        if (size > 0) {
            implicitHeaderMode();

            writeRegister(REG_PAYLOAD_LENGTH, (byte) (size & 0xff));
        } else {
            explicitHeaderMode();
        }

        // clear IRQ's
        writeRegister(REG_IRQ_FLAGS, (byte) irqFlags);

        if (((irqFlags & IRQ_RX_DONE_MASK) != 0x00) && ((irqFlags & IRQ_PAYLOAD_CRC_ERROR_MASK) == 0x00)) {
            // received a packet
            _packetIndex = 0;

            // read packet length
            if (_implicitHeaderMode > 0) {
                packetLength = readRegister(REG_PAYLOAD_LENGTH);
            } else {
                packetLength = readRegister(REG_RX_NB_BYTES);
            }

            // set FIFO address to current RX address
            writeRegister(REG_FIFO_ADDR_PTR, readRegister(REG_FIFO_RX_CURRENT_ADDR));

            // put in standby mode
            idle();
        } else if ((byte) readRegister(REG_OP_MODE) == (byte) (MODE_LONG_RANGE_MODE | MODE_RX_SINGLE)) {
            // not currently in RX mode

            // reset FIFO address
            writeRegister(REG_FIFO_ADDR_PTR, (byte) 0);

            // put in single RX mode
            writeRegister(REG_OP_MODE, (byte) (MODE_LONG_RANGE_MODE | MODE_RX_SINGLE));
        }

        return packetLength;

    }

    public int packetRssi()
    {
        return (readRegister(REG_PKT_RSSI_VALUE) - (_frequency < 868E6 ? 164 : 157));
    }

    public float packetSnr()
    {
        return (float) ((readRegister(REG_PKT_SNR_VALUE)) * 0.25);
    }

    public long packetFrequencyError()
    {
        int freqError = 0;
        freqError = (int)(readRegister(REG_FREQ_ERROR_MSB) & 0B111);
        freqError <<= 8L;
        freqError += (int)(readRegister(REG_FREQ_ERROR_MID));
        freqError <<= 8L;
        freqError += (int)(readRegister(REG_FREQ_ERROR_LSB));

        if ((readRegister(REG_FREQ_ERROR_MSB) & 0B1000) != 0B0000) { // Sign bit is on
            freqError -= 524288; // B1000'0000'0000'0000'0000
        }

        float fXtal = (float) 32E6; // FXOSC: crystal oscillator (XTAL) frequency (2.5. Chip Specification, p. 14)
        float fError = (((float)(freqError) * (1L << 24)) / fXtal) * (getSignalBandwidth() / 500000.0f); // p. 37

        return (long)(fError);
    }

    public long write(byte[] bytes)
    {
        return write(bytes, bytes.length);
    }

    public long write(byte[] buffer, long size)
    {
        int currentLength = readRegister(REG_PAYLOAD_LENGTH);

        // check size
        if ((currentLength + size) > MAX_PKT_LENGTH) {
            size = MAX_PKT_LENGTH - currentLength;
        }

        // write data
        for (int i = 0; i < size; i++) {
            writeRegister(REG_FIFO, buffer[i]);
        }

        // update length
        writeRegister(REG_PAYLOAD_LENGTH, (byte) (currentLength + size));

        return size;
    }

    public int available()
    {
        return (readRegister(REG_RX_NB_BYTES) - _packetIndex);
    }

    public int read()
    {
        if (available() <= 0) {
            return -1;
        }

        _packetIndex++;

        return readRegister(REG_FIFO);
    }

    public int peek()
    {
        if (available() <= 0) {
            return -1;
        }

        // store current FIFO address
        int currentAddress = readRegister(REG_FIFO_ADDR_PTR);

        // read
        byte b = readRegister(REG_FIFO);

        // restore FIFO address
        writeRegister(REG_FIFO_ADDR_PTR, (byte) currentAddress);

        return b;
    }

    public void flush(){}

    private void explicitHeaderMode()
    {
        _implicitHeaderMode = 0;

        writeRegister(REG_MODEM_CONFIG_1, (byte) (readRegister(REG_MODEM_CONFIG_1) & 0xfe));
    }

    private void implicitHeaderMode()
    {
        _implicitHeaderMode = 1;

        writeRegister(REG_MODEM_CONFIG_1, (byte) (readRegister(REG_MODEM_CONFIG_1) | 0x01));
    }

    private void handleDio0Rise()
    {
        int irqFlags = readRegister(REG_IRQ_FLAGS);

        // clear IRQ's
        writeRegister(REG_IRQ_FLAGS, (byte) irqFlags);

        if ((irqFlags & IRQ_PAYLOAD_CRC_ERROR_MASK) == 0) {
            // received a packet
            _packetIndex = 0;

            // read packet length
            int packetLength = (_implicitHeaderMode > 0) ? readRegister(REG_PAYLOAD_LENGTH) : readRegister(REG_RX_NB_BYTES);

            // set FIFO address to current RX address
            writeRegister(REG_FIFO_ADDR_PTR, readRegister(REG_FIFO_RX_CURRENT_ADDR));

            // reset FIFO address
            writeRegister(REG_FIFO_ADDR_PTR, (byte) 0);
        }
    }


    private int getSpreadingFactor()
    {
        return readRegister(REG_MODEM_CONFIG_2) >> 4;
    }

    public void setSpreadingFactor(int sf)
    {
        if (sf < 6) {
            sf = 6;
        } else if (sf > 12) {
            sf = 12;
        }

        if (sf == 6) {
            writeRegister(REG_DETECTION_OPTIMIZE, (byte) 0xc5);
            writeRegister(REG_DETECTION_THRESHOLD, (byte) 0x0c);
        } else {
            writeRegister(REG_DETECTION_OPTIMIZE, (byte) 0xc3);
            writeRegister(REG_DETECTION_THRESHOLD, (byte) 0x0a);
        }

        writeRegister(REG_MODEM_CONFIG_2, (byte) ((readRegister(REG_MODEM_CONFIG_2) & 0x0f) | ((sf << 4) & 0xf0)));
        setLdoFlag();
    }

    private long getSignalBandwidth()
    {
        byte bw = (byte) (readRegister(REG_MODEM_CONFIG_1) >> 4);

        switch (bw) {
            case 0: return 7800;
            case 1: return 10400;
            case 2: return 15600;
            case 3: return 20800;
            case 4: return 31250;
            case 5: return 41700;
            case 6: return 62500;
            case 7: return 125000;
            case 8: return 250000;
            case 9: return 500000;
        }

        return -1;
    }

    public void setSignalBandwidth(long sbw)
    {
        int bw;

        if (sbw <= 7800) {
            bw = 0;
        } else if (sbw <= 10400) {
            bw = 1;
        } else if (sbw <= 15600) {
            bw = 2;
        } else if (sbw <= 20800) {
            bw = 3;
        } else if (sbw <= 31250) {
            bw = 4;
        } else if (sbw <= 41700) {
            bw = 5;
        } else if (sbw <= 62500) {
            bw = 6;
        } else if (sbw <= 125000) {
            bw = 7;
        } else if (sbw <= 250000) {
            bw = 8;
        } else /*if (sbw <= 250E3)*/ {
            bw = 9;
        }

        writeRegister(REG_MODEM_CONFIG_1, (byte) ((readRegister(REG_MODEM_CONFIG_1) & 0x0f) | (bw << 4)));
        setLdoFlag();
    }

    private void setLdoFlag()
    {
        // Section 4.1.1.5
        long symbolDuration = 1000 / ( getSignalBandwidth() / (1L << getSpreadingFactor()) ) ;

        // Section 4.1.1.6
        boolean ldoOn = symbolDuration > 16;

        byte config3 = readRegister(REG_MODEM_CONFIG_3);
//        bitWrite(config3, 3, ldoOn);
        writeRegister(REG_MODEM_CONFIG_3, config3);
    }

    public void setCodingRate4(int denominator)
    {
        if (denominator < 5) {
            denominator = 5;
        } else if (denominator > 8) {
            denominator = 8;
        }

        int cr = denominator - 4;

        writeRegister(REG_MODEM_CONFIG_1, (byte) ((readRegister(REG_MODEM_CONFIG_1) & 0xf1) | (cr << 1)));
    }

    public void setPreambleLength(long length)
    {
        writeRegister(REG_PREAMBLE_MSB, (byte) (length >> 8));
        writeRegister(REG_PREAMBLE_LSB, (byte) (length >> 0));
    }

    public void setSyncWord(int sw)
    {
        writeRegister(REG_SYNC_WORD, (byte) sw);
    }

    private void enableCrc()
    {
        writeRegister(REG_MODEM_CONFIG_2, (byte) (readRegister(REG_MODEM_CONFIG_2) | 0x04));
    }

    private void disableCrc()
    {
        writeRegister(REG_MODEM_CONFIG_2, (byte) (readRegister(REG_MODEM_CONFIG_2) & 0xfb));
    }

    public void crc() {
        enableCrc();
    }

    public void noCrc() {
        disableCrc();
    }

    private void enableInvertIQ()
    {
        writeRegister(REG_INVERTIQ,  (byte) 0x66);
        writeRegister(REG_INVERTIQ2, (byte) 0x19);
    }

    public void disableInvertIQ()
    {
        writeRegister(REG_INVERTIQ,  (byte) 0x27);
        writeRegister(REG_INVERTIQ2, (byte) 0x1d);
    }

    public byte random()
    {
        return readRegister(REG_RSSI_WIDEBAND);
    }

    private void onDio0Rise()
    {
        handleDio0Rise();
    }

    /***************************************************************
     *
     *                    RH Generic Driver Library
     *
     ***************************************************************/

    public boolean isAvailable()
    {
        if(isTransmitting() == true) return false;
        else return true;
    }

    public void waitAvailable()
    {
        while (!isAvailable()) yield();
    }

    public boolean waitAvailableTimeout(int timeout)
    {
        Calendar rightNow = Calendar.getInstance();
        long starttime = rightNow.getTimeInMillis();
        while ((rightNow.getTimeInMillis() - starttime) < timeout){
            if (isAvailable())
            {
                return true;
            }
            yield();
        }
        return false;
    }

    public boolean waitPacketSent(int timeout)
    {
        if (timeout < 0) timeout = 0;
        if (timeout == 0) {
            while ((readRegister(REG_OP_MODE) & MODE_TX) == MODE_TX) yield();
            return true;
        } else {
            Calendar rightNow = Calendar.getInstance();
            long starttime = rightNow.getTimeInMillis();
            while ((rightNow.getTimeInMillis() - starttime) < timeout){
                if ((readRegister(REG_OP_MODE) & MODE_TX) != MODE_TX)
                {
                    return true;
                }
                yield();
            }
            return false;
        }
    }
}
