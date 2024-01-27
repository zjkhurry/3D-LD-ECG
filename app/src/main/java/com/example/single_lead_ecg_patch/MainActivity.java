package com.example.single_lead_ecg_patch;

import static java.lang.Math.cos;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btSearch;
    private Button btClean;
    private Button btCap;

    public static LineChart lineChart;
    private List<Entry> list=new ArrayList<>();
    private LineDataSet lineDataSet_na;
    private LineDataSet lineDataSet_na2;
    private LineData lineData;
    private List<ILineDataSet> lineDataSets = new ArrayList<>();
    public static final String BT_UUID = "0000FFF0-0000-1000-8000-00805F9B34FB";//uuid
    public static final String serviceUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    public static final String characteristicUUID = "0000fff4-0000-1000-8000-00805f9b34fb";
    public static final String descriptorUUID = "00002902-0000-1000-8000-00805f9b34fb";
    List<String> device = Arrays.asList("59","75","27","73","20","4c","61","62","20","50","65","72","69","70","68","65","72","61","6c");
    public String filename;
    public CardView curveCard;

    private boolean flag = false;
    private float j=0;
    private FileHelper filehelper;
    private float ecg_last=0;
    public int N=0;
    public float[] wind;
    public float[] data_ecg;
    private int update=0;


    private IConnectCallback periodScanCallback = new IConnectCallback() {
        @Override
        public void onConnectSuccess(DeviceMirror deviceMirror) {
            btSearch.setText("Disconnect");
            filename = newFileName();
            initLineDataSet("Sweat", getResources().getColor(R.color.pink_700));
            BluetoothGattChannel bluetoothGattChannel = new BluetoothGattChannel.Builder()
                    .setBluetoothGatt(deviceMirror.getBluetoothGatt())
                    .setPropertyType(PropertyType.PROPERTY_NOTIFY)
                    .setServiceUUID(UUID.fromString(serviceUUID))
                    .setCharacteristicUUID(UUID.fromString(characteristicUUID))
                    .setDescriptorUUID(UUID.fromString(descriptorUUID))
                    .builder();
            deviceMirror.bindChannel(new IBleCallback() {
                @Override
                public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                    deviceMirror.setNotifyListener(bluetoothGattChannel.getGattInfoKey(), new IBleCallback() {
                        @Override
                        public void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice) {
                            ViseLog.i("notify success:" + HexUtil.encodeHexStr(data));
                            try {
                                updateLineChart(HexUtil.encodeHexStr(data));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(BleException exception) {

                        }
                    });
                }

                @Override
                public void onFailure(BleException exception) {

                }
            }, bluetoothGattChannel);
            deviceMirror.registerNotify(false);
        }

        @Override
        public void onConnectFailure(BleException exception) {
            btSearch.setText("Search BLE");
            flag = false;
        }

        @Override
        public void onDisconnect(boolean isActive) {
            btSearch.setText("Search BLE");
            flag = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        //蓝牙相关配置修改
        ViseBle.config()
                .setScanTimeout(10*1000)//扫描超时时间，这里设置为永久扫描
                .setConnectTimeout(10 * 1000)//连接超时时间
                .setOperateTimeout(5 * 1000)//设置数据操作超时时间
                .setConnectRetryCount(3)//设置连接失败重试次数
                .setConnectRetryInterval(1000)//设置连接失败重试间隔时间
                .setOperateRetryCount(3)//设置数据操作失败重试次数
                .setOperateRetryInterval(1000)//设置数据操作失败重试间隔时间
                .setMaxConnectCount(1);//设置最大连接设备数量
        //蓝牙信息初始化，全局唯一，必须在应用初始化时调用
        ViseBle.getInstance().init(this);
        btSearch = findViewById(R.id.search);
        btClean = findViewById(R.id.clean);
        lineChart = findViewById(R.id.curveChart);
        initLineChart();

        Blackman((float) (0.17*3.14), (float) (0.25*3.14));
        wind = get_win();
        data_ecg = new float[wind.length];

        curveCard = findViewById(R.id.curvecard);

        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag)
                {
                    btSearch.setText("Search BLE");
                    ViseBle.getInstance().disconnect();
                    flag = false;
                }
                else
                {
                    //该方式是扫到指定设备就停止扫描
                    String deviceName = "Yu's Lab Peripheral";
                    btSearch.setText("Connecting");
                    ViseBle.getInstance().connectByName(deviceName, periodScanCallback);
                    flag = true;
                }

            }
        });

        btClean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lineChart.clear();
//                initLineChart();
                lineDataSets.clear();
                j=0;
                initLineDataSet("Sweat", getResources().getColor(R.color.pink_700));
                //删除文件
                File file = new File(filename);
                if (file.exists()) {
                    file.delete();
                }
            }
        });
    }

    /**
     * Init line chart
     */
    private void initLineChart() {
        //lineChart.setOnChartGestureListener(this);
        lineChart.getDescription().setEnabled(false);//不显示描述
//        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);//允许缩放
        lineChart.setPinchZoom(true);
        lineChart.getLegend().setEnabled(false);
//        lineChart.getLegend().setXOffset(40);
//        lineChart.setNoDataText("No data available");
//        lineChart.setNoDataTextColor(Color.GRAY);

        //自定义适配器，适配于X轴
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setTypeface(mTfLight);
//        xAxis.setGranularity(1f);
//        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setDrawGridLines(true);
        xAxis.setDrawAxisLine(true);
//        xAxis.setTextColor(Color.BLACK);
        xAxis.setTextSize(12);
//        xAxis.setTextColor(Color.TRANSPARENT);

        YAxis leftAxis = lineChart.getAxisLeft();
        //leftAxis.setTypeface(mTfLight);
        leftAxis.setLabelCount(8, false);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
//        leftAxis.setAxisMinimum(261.94f);//(4.2f);
//        leftAxis.setAxisMaximum(261.95f);//(4.8f);
        leftAxis.setSpaceTop(15f);
//        leftAxis.setTextColor(getResources().getColor(R.color.purple_A100));
        leftAxis.setTextSize(12);
        leftAxis.setDrawGridLines(true);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setEnabled(true);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisRight().setDrawAxisLine(false);
//        lineChart.animateX(2000);

    }

    private void initLineDataSet(String name, int color) {
        lineDataSet_na = new LineDataSet(null, "ECG");
        lineDataSet_na.setLineWidth(2f);
        //lineDataSet_na[i].setCircleRadius(1.5f);
        lineDataSet_na.setDrawValues(false);
        lineDataSet_na.setDrawCircles(false);
        lineDataSet_na.setColor(getResources().getColor(R.color.orange_200));
        //lineDataSet_na[i].setCircleColor(color);
        lineDataSet_na.setHighLightColor(getResources().getColor(R.color.orange_200));
        //设置曲线填充
//            lineDataSet_na[i].setDrawFilled(true);
//            lineDataSet_na[i].setFillDrawable(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
//                    new int[]{getResources().getColor(R.color.amber_200_30), getResources().getColor(R.color.transparent)}));

        lineDataSet_na.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet_na.setValueTextSize(10f);
        lineDataSet_na.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSets.add(lineDataSet_na);
        //添加一个空的 LineData
        lineData = new LineData();
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    public void Blackman(float Wp, float Ws)
    {
        int i;
        float v = Ws - Wp;
        float n= (float) ((5.5*2*3.14)/v);
        for(i=0;i<n;i++);
        N=i;
    }
    public float[] get_win()
    {
        int n;
        float[] win=new float[N];
        for(n=0;n<N;n++)
        {
            win[n]= (float) (0.42-0.5*cos(2*3.14*n/(N-1))+0.08*cos(4*3.14*n/(N-1)));
        }
        return win;
    }

    public void updateLineChart(String data) throws InterruptedException {

        int ind = 796;

        String ecg;//+data.substring(0,2);
        float temp = 0;
        int ecg_i = 0;
//        ecg_i = Integer.parseInt(ecg,16);

        if (lineDataSet_na.getEntryCount() == 0) {
            lineData.addDataSet(lineDataSet_na);
        }

//        lineData.addEntry(new Entry((float) (j/20.0), (float)ecg_i),0);
        for (int i = 0; i < 6; i++) {
            ecg = data.substring(i * 6, i * 6 + 6);
            ecg_i = Integer.parseInt(ecg, 16);
            if (ecg_i > 8388607)
                ecg_i = ecg_i - 16777216;
//            for(int k=0;k<N-1;k++) {
//                data_ecg[k]=data_ecg[k+1];
//                temp=temp+data_ecg[k]*wind[k];
//            }
//            data_ecg[N-1]=(float)ecg_i;
//            temp=temp+data_ecg[N-1]*wind[N-1];
            Entry data0 = new Entry((float) (j), (float) ecg_i);
            lineData.addEntry(data0, 0);
            save(filename, String.valueOf(ecg_i));
            j += 1 / 120.0;
            while (lineDataSet_na.getEntryCount() > 1200)
                lineDataSet_na.removeFirst();
        }

//        ecg_last= ecg_i;
//        if(j>1200) {
//            lineDataSet_na.removeFirst();
//            lineDataSet_na.removeFirst();
//            lineDataSet_na.removeFirst();
//            lineDataSet_na.removeFirst();
//            lineDataSet_na.removeFirst();
//        }


//        save(filename,String. valueOf(ecg_i));
        update++;
        if (update == 3) {
            lineChart.setData(lineData);
            lineData.notifyDataChanged();
            lineChart.notifyDataSetChanged();
//            lineChart.setVisibleXRangeMaximum(600);
            lineChart.moveViewToX(lineData.getEntryCount() - 600);
//            lineChart.invalidate();
            update = 0;
        }

    }

    private void requestMyPermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //没有授权，编写申请权限代码
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }
    /**
     * get file name such as 20171031.txt
     *
     * @return
     */
    public String newFileName() {
        String path = "Android/data/com.example.sleeping_monitoring/";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/"
                    + "Crash/";
        }
        requestMyPermissions();
        File file = new File(path);
        if (!file.exists()) {
            boolean isCreateRoot=file.mkdirs();
        }
        StringBuilder sb = new StringBuilder();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.CHINA);
        String now = sdf.format(new Date());
        File saveFile = new File(path+now.toString()+".txt");
        try {
            saveFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path+now.toString()+".txt";
    }

    /**
     * 定义文件保存的方法，写入到文件中，所以是输出流
     */
    public void save(String FileName, String ecg) {
        String content =ecg;
        FileOutputStream fos = null;
        try {
            // Context.MODE_PRIVATE私有权限，Context.MODE_APPEND追加写入到已有内容的后面
            fos = new FileOutputStream(FileName, true);
//            fos = openFileOutput(FileName, Context.MODE_APPEND);
            fos.write(content.getBytes());
            fos.write("\r\n".getBytes());//写入换行
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}