 ```
gcloud functions deploy sensor-consumer --region=europe-west1 --entry-point dk.elkjaerit.smartheating.functions.Consumer --runtime java11 --trigger-topic=my-topic --allow-unauthenticated
gcloud functions deploy power-unit-scheduling --region=europe-west1 --entry-point dk.elkjaerit.smartheating.functions.PowerUnitUpdateFunction --runtime java11 --trigger-topic=power-unit-scheduler
adju
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