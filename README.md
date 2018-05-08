# react-native-IoTWifiHandler

Wifi Handler for IoT devices in Android for Supplicant state anamoly. In Android, if you are building an application for WiFi connectivity and IoT, Android might push the SSID to supplicant state and not give any response for further connection. This plugin handles the supplicant state and ensures that connectivity happens with the right peer.

## Setup

This library is available on npm, install it with: `npm install --save https://github.com/studiosdeterminant/IoTWifiHandler.git`

## Usage

1. Import react-native-IoTWifiHandler:

```javascript
import IoTWifiHandler from 'react-native-IoTWifiHandler';
```

2. Register a DeviceEmitter in the following way:

```javascript
DeviceEventEmitter.addListener('supplicantStateChange', function(e: Event) {
    if(e.status == "COMPLETED" && e.ssid != undefined){
        if(e.ssid.toString()){
            DeviceEventEmitter.removeAllListeners('supplicantStateChange');
            IoTWifiHandler.bindProcessToWifi();
        }else{
            alert("Connection failed, try again!");
            DeviceEventEmitter.removeAllListeners('supplicantStateChange');
        }
    }else if (e.status == "ERROR_AUTHENTICATING" && e.ssid == undefined) {
        DeviceEventEmitter.removeAllListeners('supplicantStateChange');
        alert("Authentication Error! Verify Password");
    }else{
      //Do nothing
    }
});
```

3. Functions

```javascript
  IoTWifiHandler.bindProcessToWifi()
  IoTWifiHandler.unbindProcessToNetwork()
```

4. SSID states

ASSOCIATED
ASSOCIATING
AUTHENTICATING
COMPLETED
DISCONNECTED
DORMANT
FOUR_WAY_HANDSHAKE
GROUP_HANDSHAKE
INACTIVE
INTERFACE_DISABLED
INVALID
SCANNING
UNINITIALIZED

This is a WIP. Pull requests, feedbacks and suggestions are welcome!
