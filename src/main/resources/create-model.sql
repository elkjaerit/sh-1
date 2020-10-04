CREATE OR REPLACE MODEL
  `%s`
OPTIONS
  ( MODEL_TYPE='LOGISTIC_REG',
    AUTO_CLASS_WEIGHTS=TRUE,
    input_label_cols=['label'] ) AS
SELECT
  cloud_bucket, zenith_bucket, azimuth_bucket, label
FROM
(
SELECT
 created,
 round(AVG(clouds) OVER (rolling_six_days)/20)*20 cloud_bucket,
 round(AVG(azimuth) OVER (rolling_six_days)/5)*5 azimuth_bucket,
 round(AVG(zenith) OVER (rolling_six_days)/5)*5 zenith_bucket,
 CASE
   WHEN LAST_VALUE(temperature) over (rolling_six_days) - FIRST_VALUE(temperature) over (rolling_six_days) > 0.7 THEN 'HIGH_INC'
   ELSE 'NONE'
  END label,
  dataframe
FROM
  ( select  roomId, created, temperature, azimuth, zenith, clouds,
   CASE
    WHEN MOD(ABS(FARM_FINGERPRINT(CAST(created AS STRING))),10) < 8 THEN 'training'
    WHEN MOD(ABS(FARM_FINGERPRINT(CAST(created AS STRING))),10) = 8 THEN 'evaluation'
    WHEN MOD(ABS(FARM_FINGERPRINT(CAST(created AS STRING))),10) = 9 THEN 'prediction'
  END AS dataframe,
  TIMESTAMP_DIFF(created, TIMESTAMP "2010-07-07 10:20:00+00", MINUTE) time_diff
  from `sensors.sensordata`)
  where roomId='%s'
  WINDOW rolling_six_days AS
  (partition by roomId ORDER BY time_diff RANGE BETWEEN 180 PRECEDING AND CURRENT ROW)

)
where dataframe = 'training'
