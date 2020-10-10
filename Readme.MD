 ```
gcloud functions deploy sensor-consumer --region=europe-west1 --entry-point dk.elkjaerit.smartheating.functions.Consumer --runtime java11 --trigger-topic=my-topic --allow-unauthenticated
gcloud functions deploy power-unit-scheduling --region=europe-west1 --entry-point dk.elkjaerit.smartheating.functions.PowerUnitUpdateFunction --runtime java11 --trigger-topic=power-unit-scheduler
gcloud functions deploy weather-scheduling --region=europe-west1 --entry-point dk.elkjaerit.smartheating.functions.WeatherUpdaterFunction --runtime java11 --trigger-topic=weather-scheduler
```
 
 
 SQL til moving average:
 
    https://stackoverflow.com/questions/49883311/bigquery-moving-average-with-missing-values
 
 Sensors:
 


    IDA:        58:2D:34:35:DB:12
    Mathilde:   58:2D:34:35:DB:07 *
    Alrum:      58:2D:34:35:D8:73
    Lille bad:  58:2D:34:35:CC:C4
    Kontor:     58:2D:34:35:CB:89
    Stue:       58:2D:34:35:D5:A0
    Bad:        58:2D:34:35:AE:31
    Bryggers:   
    


https://medium.com/google-developers/building-a-smart-home-cloud-service-with-google-1ee436ac5a03

Gode eksempler
https://codinginflow.com/tutorials/android/cloud-firestore/part-17-nested-objects

ML Learnings
https://towardsdatascience.com/machine-learning-for-data-analysts-bigquery-ml-b60ef05e43c2
https://developers.google.com/machine-learning/crash-course/classification/video-lecture

# Udeståender
* Import data fra gammel DB
* Indsætte data i korrekt tabel efter import
* Function som kan lave ML Modeller for alle rum
* Send til Power Unit 
    * Leg med Jentroniks kode
    * Sæt Google iot op
    * Opret scheduleret function til at send status til Power Unit
    
    
    


180 + 0.7 : God




15:29:32.934 -> {"deviceId": "B93A7D80", "mac": "58:2D:34:35:DB:12", "temp": 24.1, "humidity": 48.9, "rssi": -90, "timestamp": 1602163773}
15:29:37.986 -> E (3740829) task_wdt: Task watchdog got triggered. The following tasks did not reset the watchdog in time:
15:29:37.986 -> E (3740829) task_wdt:  - IDLE0 (CPU 0)
15:29:37.986 -> E (3740829) task_wdt: Tasks currently running:
15:29:37.986 -> E (3740829) task_wdt: CPU 0: Btc_task
15:29:37.986 -> E (3740829) task_wdt: CPU 1: IDLE1
15:29:37.986 -> E (3740829) task_wdt: Aborting.
15:29:37.986 -> abort() was called at PC 0x400de737 on core 0
15:29:37.986 -> 
15:29:37.986 -> Backtrace: 0x40091f60:0x3ffbe320 0x40092191:0x3ffbe340 0x400de737:0x3ffbe360 0x40084805:0x3ffbe380 0x4008e465:0x3ffed400 0x40151e91:0x3ffed440 0x4013c257:0x3ffed460 0x4013bad9:0x3ffed490 0x4013bff6:0x3ffed4b0 0x4013f960:0x3ffed4f0 0x40140145:0x3ffed520 0x401402b6:0x3ffed540 0x40123ab3:0x3ffed560 0x4000bd83:0x3ffed580 0x4000182a:0x3ffed5a0 0x400f7f78:0x3ffed5c0 0x400ebb92:0x3ffed5f0 0x400ed0d6:0x3ffed610 0x400ed13f:0x3ffed630 0x400d4234:0x3ffed650 0x400d3b01:0x3ffed670 0x401e5135:0x3ffed690 0x401e5206:0x3ffed6b0 0x400d4a98:0x3ffed6e0 0x400d453a:0x3ffed740 0x400d58f9:0x3ffed7b0 0x400d59a3:0x3ffed7d0 0x400d1f43:0x3ffed800 0x400d21d3:0x3ffed830 0x400d24bd:0x3ffed920 0x400da461:0x3ffed9f0 0x400d9202:0x3ffedae0 0x4015cac9:0x3ffedb30 0x40157bda:0x3ffedb50 0x4008e6a9:0x3ffedb80
15:29:38.021 -> 
15:29:38.021 -> Rebooting...
15:29:38.054 -> ets Jun  8 2016 00:22:57
15:29:38.054 -> 
15:29:38.054 -> rst:0xc (SW_CPU_RESET),boot:0x13 (SPI_FAST_FLASH_BOOT)
15:29:38.054 -> configsip: 0, SPIWP:0xee
15:29:38.054 -> clk_drv:0x00,q_drv:0x00,d_drv:0x00,cs0_drv:0x00,hd_drv:0x00,wp_drv:0x00
15:29:38.054 -> mode:DIO, clock div:1
15:29:38.054 -> load:0x3fff0018,len:4
15:29:38.054 -> load:0x3fff001c,len:1216
15:29:38.054 -> ho 0 tail 12 room 4
15:29:38.054 -> load:0x40078000,len:9720
15:29:38.054 -> ho 0 tail 12 room 4
15:29:38.054 -> load:0x40080400,len:6352
15:29:38.054 -> entry 0x400806b8
15:29:38.534 -> Starting wifi
15:29:38.673 -> Connecting to WiFi
15:29:38.878 -> Waiting on time sync...
15:29:38.878 -> checking wifi...
15:29:38.878 -> WiFi Connected
15:29:38.878 -> IP address: 
