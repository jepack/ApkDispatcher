# Apk Dispatcher 
## Function profile
Support several mode of dispatch your APKs to Devices:
1. APP Push: Base on "Heart Beat" and "Daemon Service" technology， and you can change the heart beat frequency in settings page.
2. Server Cache App: You can give an list of app through file "app_cache.json" on your server. The server can be Lan or Internet.
3. Input APK address: You can specify the apk address by input, and click the install button. It will be recorded into local storage history.
4. You also can add or remove APK address with a simple server api, based on NanoHttpD server:

  * Add a local pkg file addreass
  
    http://$Device_IP:8909/api/add_url
  
  * Remove a local pkg file addreass
  
    http://$Device_IP:8909/api/rm_url  
  
  * Get current push_flag of the device
  
    http://$Device_IP:8909/api/push_flag
  
## Server json example

The json address can be any which can be download directly.

> app_push.json:

```json
{
        "flag": 1,
        "pkg": "com.jepack.dispatcher",
        "url": "http://192.168.31.92/dispatcher.apk",
        "md5": "91377cf93d7c510f745d2a7192602245",
        "app_code": 1,
        "force": true,
        "title": "APK Dispatcher 0.1.0",
        "desc": "Function done!"
}
```


> app_cache.json:

```json
[
  {
          "url": "http://192.168.1.217/cache.apk",
          "md5": "fe4aa390ba7fb86fa05256bd2bd8d1a2 "
  },
   {
          "url": "http://192.168.1.217/cache1.apk",
          "md5": "fe4aa390ba7fb86fa05256bd2bd8d1a2 "
  }
]
```

## TODO

1. Show app icon in app push notification and cache list.
2. Show abundant info in cache list.
3. Show progress of background downloading app
4. Add the English language translation
