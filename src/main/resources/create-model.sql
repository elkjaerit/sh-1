CREATE OR REPLACE MODEL
  `%s`
OPTIONS
  ( MODEL_TYPE='LOGISTIC_REG',
    AUTO_CLASS_WEIGHTS=TRUE,
    EARLY_STOP=TRUE,
    MIN_REL_PROGRESS=0.001,
    MIN_SPLIT_LOSS=0.1,
    input_label_cols=['label'] ) AS
SELECT
  cloud_bucket, zenith_bucket, azimuth_bucket, label
FROM
(
SELECT
 created,
 round(AVG(clouds) OVER (rolling_six_days)/10)*10 cloud_bucket,
 round(AVG(azimuth) OVER (rolling_six_days)/5)*5 azimuth_bucket,
 round(AVG(zenith) OVER (rolling_six_days)/5)*5 zenith_bucket,
 CASE
   WHEN LAST_VALUE(temperature) over (rolling_six_days) - FIRST_VALUE(temperature) over (rolling_six_days) > 1 THEN 'HIGH_INC'
   ELSE 'NONE'
  END label
FROM
  ( select  roomId, created, temperature, azimuth, zenith, clouds,
  TIMESTAMP_DIFF(created, TIMESTAMP "2010-07-07 10:20:00+00", MINUTE) time_diff
  from `sensors.sensor`)
  where roomId='%s' and created > TIMESTAMP "2020-09-01 00:00:00+00"
  WINDOW rolling_six_days AS
  (partition by roomId ORDER BY time_diff RANGE BETWEEN 120 PRECEDING AND CURRENT ROW)
)
