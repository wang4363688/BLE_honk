package com.swijaya.sampleble;

/**
 * Created by Murphy on 16/9/25.
 */
public class NewlyBluetoothDevice {
    private String macAddress;
    private String adInfo;

    public NewlyBluetoothDevice(String macAddress, String adInfo){
        this.macAddress = macAddress;
        this.adInfo = adInfo;
    }

    public String getMacAddress(){
        return macAddress;
    }

    public String getAdInfo(){
        return adInfo;
    }
}
