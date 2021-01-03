select * from
ML.CONFUSION_MATRIX(MODEL `sensors.newyear_20_120`, (
select cloud_bucket, azimuth_bucket, zenith_bucket, label from (
SELECT

 case when AVG(clouds) OVER (rolling_six_days) > 50 THEN 'CLOUDY' else 'SUNNY' end cloud_bucket,
 round(AVG(azimuth) OVER (rolling_six_days)/5)*5 azimuth_bucket,
 round(AVG(zenith) OVER (rolling_six_days)/5)*5 zenith_bucket,
 CASE
   WHEN LAST_VALUE(temperature) over (rolling_six_days) - FIRST_VALUE(temperature) over (rolling_six_days) > 0.7 THEN 'HIGH_INC'
   ELSE 'NONE'
  END label,
  dataframe,
FROM

(
select  roomId, created, temperature, azimuth, zenith, clouds,
   CASE
    WHEN MOD(ABS(FARM_FINGERPRINT(CAST(created AS STRING))),10) < 8 THEN 'training'
    WHEN MOD(ABS(FARM_FINGERPRINT(CAST(created AS STRING))),10) = 8 THEN 'evaluation'
    WHEN MOD(ABS(FARM_FINGERPRINT(CAST(created AS STRING))),10) = 9 THEN 'prediction'
  END AS dataframe,
  TIMESTAMP_DIFF(created, TIMESTAMP "2010-07-07 10:20:00+00", MINUTE) time_diff
  from `sensors.sensor`
  where roomId='ca306115-0050-472c-b2dc-652247bf5342' and created between TIMESTAMP "2020-09-01 00:00:00+00" AND TIMESTAMP "2020-12-31 00:00:00+00"
  )
  WINDOW rolling_six_days AS
  (partition by roomId ORDER BY time_diff RANGE BETWEEN 120 PRECEDING AND CURRENT ROW)

  )
  where dataframe='training'
  ), STRUCT(%s AS threshold))
