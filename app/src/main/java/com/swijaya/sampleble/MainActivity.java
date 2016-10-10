package com.swijaya.sampleble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
    //GPS相关
    private LocationManager mLocationManager;
    private Location mLocation;
    private double lat;
    private double lng;
    private double time;
    private double speed;
    private double bearing;
    String provider;
    private Criteria criteria = new Criteria();
    private String VEHICLES = "V";
    private String PEDESTRIAN = "P";
    private double latShow, lngShow, speedShow, bearingShow;
    /*GPS Info Formatting
    * speed:   4 charaters
    * bearing: 3 charaters
    * lat/lng: 8 charaters
    */
    DecimalFormat dfSpeed = new DecimalFormat("00.0");
    DecimalFormat dfBear = new DecimalFormat("000");
    DecimalFormat dfLa = new DecimalFormat("0.00000");

    private static final String TAG = "SampleBLE";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int DEFAULT_ADVERTISE_INTERVAL = 500;
    private static final int INTERVAL_BETWEEN_ONANDOFF = 300;
    private static final ParcelUuid SAMPLE_UUID =
            ParcelUuid.fromString("0000FE00-0000-1000-8000-00805F9B34FB");

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ArrayList<String> deviceAddress = new ArrayList<String>();
    private BluetoothAdapter mBluetoothAdapter;

    // helper objects for BLE advertising, derived from mBluetoothAdapter above
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseSettings.Builder mBleAdvertiseSettingsBuilder;
    private AdvertiseData.Builder mBleAdvertiseDataBuilder;

    // helper objects for BLE scanning, derived from mBluetoothAdapter above
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanSettings.Builder mBleScanSettingsBuilder;

    private Handler mHandler;
    private Timer timer;
    private TimerTask timerTask;
    private boolean isActive = true;

    private byte[] serviceData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = mLocationManager.getBestProvider(criteria, true); // 获取GPS信息
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        mLocation = mLocationManager.getLastKnownLocation(provider);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mLeDeviceListAdapter = new LeDeviceListAdapter(MainActivity.this);
        ListView deviceList = (ListView) findViewById(R.id.device_list);
        deviceList.setAdapter(mLeDeviceListAdapter);


        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 0x123) {
                    //do something here
                    //Toast.makeText(MainActivity.this, "timer", Toast.LENGTH_SHORT).show();
                    startAdvertising();
                    try {
                        Thread.sleep(INTERVAL_BETWEEN_ONANDOFF);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stopAdvertising();
                    //start advertising
                    //adStatus.setText("正在广播......");
                }
            }
        };

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // check for peripheral mode support
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            assert (mBluetoothLeAdvertiser != null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            finish();
            return;
        }

        // instantiate BLE advertising helper objects
        if (mBluetoothLeAdvertiser != null) {
            mBleAdvertiseSettingsBuilder = new AdvertiseSettings.Builder()
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    //.setTimeout(DEFAULT_ADVERTISE_TIMEOUT)
                    .setConnectable(false);
            mBleAdvertiseDataBuilder = new AdvertiseData.Builder()
                    .setIncludeDeviceName(false)
                    .setIncludeTxPowerLevel(true);
        }

        // instantiate BLE scanner helper objects
        if (mBluetoothLeScanner != null) {
            mBleScanSettingsBuilder = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop ongoing advertising/scanning, if any
        stopAdvertising();
        stopScanning();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // TODO
                break;
            default:
                // TODO
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                stopTimer();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopScanning();
                return true;
            case R.id.action_advertise:
                startTimer();
                //startAdvertising();
                return true;
            case R.id.action_scan:
                startScanning();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, R.string.ble_peripheral_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        /*数字的话17位刚刚不会报错
        * 构建GPS广播包
        */
        serviceData = updateGpsInfo(VEHICLES).getBytes();


        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
        final AdvertiseData advertiseData = new AdvertiseData.Builder()
                .addServiceUuid(SAMPLE_UUID)
                .addServiceData(SAMPLE_UUID, serviceData)
                .build();

        Log.d(TAG, "Starting advertising with settings:" + advertiseSettings + " and data:" + advertiseData);

        // the default settings already put a time limit of 10 seconds, so there's no need to schedule
        // a task to stop it
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mBleAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            Log.d(TAG, "Stop advertising.");
            mBluetoothLeAdvertiser.stopAdvertising(mBleAdvertiseCallback);
        }
    }

    private void startScanning() {
        assert (mBluetoothLeScanner != null);
        assert (mBleScanSettingsBuilder != null);

        // add a filter to only scan for advertisers with the given service UUID
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(SAMPLE_UUID).build()
        );

        ScanSettings bleScanSettings = mBleScanSettingsBuilder.build();

        Log.d(TAG, "Starting scanning with settings:" + bleScanSettings + " and filters:" + bleScanFilters);

        // tell the BLE controller to initiate scan
        mBluetoothLeScanner.startScan(bleScanFilters, bleScanSettings, mBleScanCallback);

        // post a future task to stop scanning after (default:25s)
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                stopScanning();
//            }
//        }, DEFAULT_SCAN_PERIOD);
    }

    private void stopScanning() {
        if (mBluetoothLeScanner != null) {
            Log.d(TAG, "Stop scanning.");
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            Toast.makeText(MainActivity.this, "扫描已关闭", Toast.LENGTH_SHORT).show();
        }
    }

    private void processScanResult(ScanResult result) {
        Log.d(TAG, "processScanResult: " + result);

        BluetoothDevice device = result.getDevice();

        Log.d(TAG, "Device name: " + device.getName());
        Log.d(TAG, "Device address: " + device.getAddress());
        Log.d(TAG, "Device service UUIDs: " + device.getUuids());

        ScanRecord record = result.getScanRecord();
        String gpsInfo = new String(record.getServiceData(SAMPLE_UUID), Charset.forName("UTF-8"));
        speedShow = Double.parseDouble(gpsInfo.substring(2,6));
        bearingShow = Double.parseDouble(gpsInfo.substring(6,9));
        latShow = Double.parseDouble("0." + gpsInfo.substring(9,13)) + 39;
        lngShow = Double.parseDouble("0." + gpsInfo.substring(13,16)) + 116;
        String gpsShow = "speed: "+ speedShow  + "m/s  bearing:" + bearingShow + "degree\nlongitude:" +
                lngShow + " latitude:" + latShow;

        Log.d(TAG, "Record advertise flags: 0x" + Integer.toHexString(record.getAdvertiseFlags()));
        Log.d(TAG, "Record Tx power level: " + record.getTxPowerLevel());
        Log.d(TAG, "Record device name: " + record.getDeviceName());
        Log.d(TAG, "Record service UUIDs: " + record.getServiceUuids());
        Log.d(TAG, "Record service data: " + record.getServiceData());
        NewlyBluetoothDevice bleDevice = new NewlyBluetoothDevice(gpsShow, gpsInfo);
        mLeDeviceListAdapter.addDevice(bleDevice);
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    private final AdvertiseCallback mBleAdvertiseCallback = new AdvertiseCallback() {

        private static final String TAG = "SampleBLE.AdvertiseCallback";

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "onStartSuccess: " + settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            String description;
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    description = "ADVERTISE_FAILED_ALREADY_STARTED";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    description = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    description = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    description = "ADVERTISE_FAILED_INTERNAL_ERROR";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    description = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                    break;
                default:
                    description = "Unknown error code " + errorCode;
                    break;
            }
            Log.e(TAG, "onStartFailure: " + description);
        }
    };

    private final ScanCallback mBleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.w("batch", "more devices");
            for (ScanResult result : results) {
                processScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String description;
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    description = "SCAN_FAILED_ALREADY_STARTED";
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    description = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    description = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    description = "SCAN_FAILED_INTERNAL_ERROR";
                    break;
                default:
                    description = "Unknown error code " + errorCode;
                    break;
            }
            Log.e(TAG, "onScanFailed: " + description);
        }
    };

    public static final String getDate() {
        Calendar cal = Calendar.getInstance();
        java.text.SimpleDateFormat sdf = new SimpleDateFormat("ss");
        String cdate = sdf.format(cal.getTime());
        System.out.println("times is :" + cdate);
        return cdate;
    }

    class UpdateTask extends TimerTask {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(0x123);
        }
    }

    public void startTimer() {
        if (isActive) {
            timer = new Timer();
            timerTask = new UpdateTask();
            if ((timer != null) && (timerTask != null)) {
                timer.schedule(timerTask, 0, DEFAULT_ADVERTISE_INTERVAL);
                isActive = false;
            }
        } else {
            Toast.makeText(MainActivity.this, "广播已开启", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopTimer() {
        if ((timer != null) && (timerTask != null)) {
            timer.cancel();
            timerTask.cancel();
//            timer = null;
//            timerTask = null;
            isActive = true;
            Toast.makeText(MainActivity.this, "广播已关闭", Toast.LENGTH_SHORT).show();
        }
    }

    public final LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
            updateToNewLocation(null);
        }

        @Override
        public void onLocationChanged(Location location) {
            updateToNewLocation(location);
        }
    };

    private Location updateToNewLocation(Location location) {
        if (location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            time = location.getTime();
            speed = location.getSpeed();
            bearing = location.getBearing();
            //String timeStamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(time);
            if (lat < 0.1 || lng < 0.1) {
                speed = 11.1f;
                lat = 111.000000000;
                lng = 111.11111111;
                bearing = 111;
            }
        } else {
            Toast.makeText(MainActivity.this, "当前无GPS信号，请移动到空旷地带！", Toast.LENGTH_SHORT).show();
        }
        return location;
    }

    private String updateGpsInfo(String Flag){
        // 以下均为本机的时间以及gps信息
        String sLatitude=String.valueOf(dfLa.format(lat));
        String sLongitude=String.valueOf(dfLa.format(lng));
        //没有gps信号的情况下不会闪退
        if(sLatitude=="0.0"||sLongitude=="0.0"){
            sLatitude="111.00000";
            sLongitude="111.11111";
        }
        //16 Charaters in total
        String rename = Flag + "1";         //所有WHIP结构均以“P”或“V”开头；时间，保留分和秒，共4位 +getDate()
        rename += String.valueOf(dfSpeed.format(speed));//速度保留一位小数，算小数点4位，单位m/s
        rename += String.valueOf(dfBear.format(bearing));//方向角取整，3位
        rename += sLatitude.substring(sLatitude.indexOf(".")+1,sLatitude.indexOf(".")+5);//纬度，只显示小数点后4位
        rename += sLongitude.substring(sLongitude.indexOf(".")+1,sLongitude.indexOf(".")+5);//经度，只显示小数点后4位
        return rename;
    }
}
