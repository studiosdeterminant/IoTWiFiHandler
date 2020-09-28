package com.determinantstudios.iotwifihandler;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Callback;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.Formatter;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.net.wifi.SupplicantState;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.ConnectivityManager;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.List;


public class IoTWifiHandlerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

	WifiManager wifiService;
	Boolean receiverRegistered = false;
	final ConnectivityManager con_manager;
	final NetworkRequest.Builder net_request;

  public IoTWifiHandlerModule(ReactApplicationContext reactContext) {
    super(reactContext);

    wifiService = (WifiManager)getReactApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    con_manager = (ConnectivityManager) getReactApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    net_request = new NetworkRequest.Builder();
  }

  //@Override
  public void onHostResume() {
    registerReceiver();
  }

  //@Override
  public void onHostPause() {
    unregisterReceiver();
  }

  //@Override
  public void onHostDestroy() {
  }

  @Override
  public void initialize() {
    getReactApplicationContext().addLifecycleEventListener(this);
  }

  @Override
  public String getName() {
    return "IoTWifiHandler";
  }

  @ReactMethod
  public void getSSID(final Callback callback) {
    callback.invoke(connectionInfo("SSID"));
  }

  @ReactMethod
  public void getBSSID(final Callback callback) {
        callback.invoke(connectionInfo("BSSID"));
    }

    @ReactMethod
    public void getRssi(final Callback callback) {
        callback.invoke(connectionInfo("Rssi"));
    }

    @ReactMethod
    public void getRxLinkSpeedMbps(final Callback callback) {
        callback.invoke(connectionInfo("LinkSpeed"));
    }

    @ReactMethod
    public void getIpAddress(final Callback callback) {
        callback.invoke(connectionInfo("IpAddress"));
    }

    @ReactMethod
    public void getFrequency(final Callback callback) {
        callback.invoke(connectionInfo("Frequency"));
    }

    @ReactMethod
    public void isWifiEnabled(final Callback callback) {
        callback.invoke(wifiService.isWifiEnabled());
    }
    @ReactMethod
    public void getWifiState(final Callback callback) {
        callback.invoke(wifiService.getWifiState());
    }
    @ReactMethod
    public void getScanResults(final Callback callback) {
        List<ScanResult> networkList = wifiService.getScanResults();
        JSONArray wifiList = new JSONArray();
        for (ScanResult networkInfo: networkList) {
//            Log.i("Aviator", networkInfo.toString());
            JSONObject network = new JSONObject();
//            Skip blank SSID
            if (networkInfo.SSID.equals("")) continue;
            try {
                network.put("BSSID", networkInfo.BSSID);
                network.put("SSID", networkInfo.SSID);
                network.put("capabilities", networkInfo.capabilities);
                network.put("level", networkInfo.level);
                network.put("frequency", networkInfo.frequency);
                network.put("timestamp", networkInfo.timestamp);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    network.put("centerFreq0", networkInfo.centerFreq0);
                    network.put("centerFreq1", networkInfo.centerFreq1);
                    network.put("channelWidth", networkInfo.channelWidth);
                    network.put("operatorFriendlyName", networkInfo.operatorFriendlyName);
                    network.put("venueName", networkInfo.venueName);
                    network.put("isPasspointNetwork", networkInfo.isPasspointNetwork());
                }
                wifiList.put(network);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        callback.invoke(wifiList.toString());
    }



//  @TargetApi(Build.VERSION_CODES.Q)
  @SuppressLint("HardwareIds")
  private String connectionInfo(String type) {
    WifiInfo wifiInformation = wifiService.getConnectionInfo();
    String information = null;
    switch (type) {
        case "SSID":
          information = wifiInformation.getSSID();
          if (information.startsWith("\"") && information.endsWith("\"")) {
              information = information.substring(1, information.length() - 1);
          }
          break;
        case "BSSID":
          information = wifiInformation.getBSSID();
          break;
        case "IpAddress":
//            ToDo: remove deprecated implementation
            information = Formatter.formatIpAddress(wifiInformation.getIpAddress());
            Log.i("Aviator", String.valueOf(information));
            break;
        case "Frequency":
            information = String.valueOf(wifiInformation.getFrequency());
            break;
        case "Rssi":
            information = String.valueOf(wifiInformation.getRssi());
            break;
        case "LinkSpeed":
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                information = String.valueOf(wifiInformation.getRxLinkSpeedMbps());
            }
            break;
    }
    return information;

  }

  private void registerReceiver() {
    IntentFilter supplicantIntentFilter = new IntentFilter();
		supplicantIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		getReactApplicationContext().registerReceiver(receiverSupplicantState, supplicantIntentFilter);
		receiverRegistered = true;
  }

  private void unregisterReceiver() {
    if(receiverRegistered){
			getReactApplicationContext().unregisterReceiver(receiverSupplicantState);
		}
  }


  @ReactMethod
  public void bindProcessToWifi() {
		net_request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
		con_manager.requestNetwork(net_request.build(), new ConnectivityManager.NetworkCallback(){
				@Override
				public void onAvailable(Network network){
					try {
              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                  con_manager.setProcessDefaultNetwork(network);
                  Log.i("Aviator", "Process bound to WiFi network");
              } else {
                  con_manager.bindProcessToNetwork(network);
                  Log.i("Aviator", "Process bound to WiFi network");
              }
          } catch (IllegalStateException e) {
              Log.e("Aviator", "ConnectivityManager bind process error ", e);
          }
				}//end of onAvailable
		});
	}

	@ReactMethod
  public void unbindProcessToNetwork() {
			try {
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
              con_manager.setProcessDefaultNetwork(null);
              Log.i("Aviator", "Process unbounded to WiFi network");
          } else {
              con_manager.bindProcessToNetwork(null);
              Log.i("Aviator", "Process unbounded to WiFi network");
          }
      } catch (IllegalStateException e) {
          Log.e("Aviator", "ConnectivityManager unbind process error ", e);
      }
	}


  private void sendSupplicantChangeEvent(WritableMap params) {
    getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("supplicantStateChange", params);
  }


  private WritableMap createSupplicantChangeEventMap(String supplicantState) {
    WritableMap event = new WritableNativeMap();
    event.putString("status", supplicantState);
    if(supplicantState.equals("COMPLETED")){
    	event.putString("ssid", connectionInfo("SSID"));
    }
    return event;
  }


  private final BroadcastReceiver receiverSupplicantState = new BroadcastReceiver() {
    @Override
    public void onReceive(Context c, Intent intent) {
        String intent_action  = intent.getAction();
        if(intent_action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
            SupplicantState supl_state=((SupplicantState)intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
            switch(supl_state){
            case ASSOCIATED:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("ASSOCIATED"));
            		Log.i("Aviator", "ASSOCIATED");
                break;
            case ASSOCIATING:Log.i("Aviator", "ASSOCIATING");
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("ASSOCIATING"));
                break;
            case AUTHENTICATING:Log.i("Aviator", "AUTHENTICATING...");
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("AUTHENTICATING"));
                break;
            case COMPLETED:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("COMPLETED"));
            		Log.i("Aviator", "COMPLETED");
                break;
            case DISCONNECTED:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("DISCONNECTED"));
            		Log.i("Aviator", "DISCONNECTED");
                break;
            case DORMANT:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("DORMANT"));
            		Log.i("Aviator", "DORMANT");
                break;
            case FOUR_WAY_HANDSHAKE:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("FOUR_WAY_HANDSHAKE"));
            		Log.i("Aviator", "FOUR_WAY_HANDSHAKE");
                break;
            case GROUP_HANDSHAKE:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("GROUP_HANDSHAKE"));
            		Log.i("Aviator", "GROUP_HANDSHAKE");
                break;
            case INACTIVE:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("INACTIVE"));
            		Log.i("Aviator", "INACTIVE");
                break;
            case INTERFACE_DISABLED:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("INTERFACE_DISABLED"));
            		Log.i("Aviator", "INTERFACE_DISABLED");
                break;
            case INVALID:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("INVALID"));
            		Log.i("Aviator", "INVALID");
                break;
            case SCANNING:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("SCANNING"));
            		Log.i("Aviator", "SCANNING");
                break;
            case UNINITIALIZED:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("UNINITIALIZED"));
            		Log.i("Aviator", "UNINITIALIZED");
                break;
            default:
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("UNKNOWN"));
            		Log.i("Aviator", "Unknown");
                break;

            }
            int suplicant_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
            if(suplicant_error == WifiManager.ERROR_AUTHENTICATING){
            		sendSupplicantChangeEvent(createSupplicantChangeEventMap("ERROR_AUTHENTICATING"));
                Log.i("Aviator", "ERROR_AUTHENTICATING");
            }
        }
    }
  };

}
