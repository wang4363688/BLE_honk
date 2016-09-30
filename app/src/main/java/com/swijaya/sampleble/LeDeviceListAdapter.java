package com.swijaya.sampleble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Murphy on 16/9/11.
 */
public class LeDeviceListAdapter extends BaseAdapter{
    //改用address作为设别标示
    //private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<String> macAddress = new ArrayList<String>();
    private ArrayList<NewlyBluetoothDevice> mLeDevices;
    private HashMap<String, NewlyBluetoothDevice> map = new HashMap<>();
    private LayoutInflater mInflator;
    public LeDeviceListAdapter(Context context) {
        super();
        //mLeDevices = new ArrayList<BluetoothDevice>();
        mLeDevices = new ArrayList<NewlyBluetoothDevice>();
        mInflator = LayoutInflater.from(context);
    }
    public void addDevice(NewlyBluetoothDevice device) {
        String did =device.getAdInfo().substring(0,2);

        map.put(did, device);
        Iterator iterator = map.keySet().iterator();
        mLeDevices.clear();
        while (iterator.hasNext()){
            String iDid = iterator.next().toString();
            mLeDevices.add(map.get(iDid));
        }
//        Log.d("TAG", String.valueOf(map.keySet().size() + "  " + did));
//        if (!macAddress.contains(did){
//            macAddress.add(device.getAdInfo().substring(0,2));
//            mLeDevices.add(device);
//        }else {
//
//            mLeDevices.clear();
//            mLeDevices.add(device);
//
//        }
//        if(!mLeDevices.contains(device)) {
//            mLeDevices.add(device);
//        }
    }
    public NewlyBluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }
    public void clear() {
        mLeDevices.clear();
    }
    @Override
    public int getCount() {
        return mLeDevices.size();
    }
    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }
    @Override
    public long getItemId(int i) {
        return i;
    }
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
//        Log.d("TAG", "get view");
//        Log.d("TAG", "map s" + map.keySet().size());

        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.adInfo = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        NewlyBluetoothDevice device = mLeDevices.get(i);
        final String deviceName = device.getMacAddress();
        if (deviceName != null && deviceName.length() > 0){
            viewHolder.deviceName.setText(deviceName);
            viewHolder.adInfo.setText(device.getAdInfo());
        } else{
            viewHolder.deviceName.setText(R.string.unknown_device);
        }
        return view;
    }
    static class ViewHolder {
        TextView deviceName;
        TextView adInfo;
    }
}