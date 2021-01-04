WITH roc as (SELECT * from ML.ROC_CURVE(MODEL `%s`,
(select
  round(AVG(clouds) OVER (rollingWindow)/10)*10 cloudBucket,
  round(AVG(azimuth) OVER (rollingWindow)/5)*5 azimuth_bucket,
  round(AVG(zenith) OVER (rollingWindow)/5)*5 zenith_bucket,
  if (LAST_VALUE(temp) over (rollingWindow) - FIRST_VALUE(temp) over (rollingWindow) > 0.7, 1,0) isTempInc,
  dataframe
FROM
  (
    select roomId,
    tenMinute,
    temp,
    azimuth,
    zenith,
    clouds,
    TIMESTAMP_DIFF(tenMinute, TIMESTAMP "2010-07-07 10:20:00+00", MINUTE) time_diff,
    CASE
        WHEN MOD(ABS(FARM_FINGERPRINT(CAST(tenMinute AS STRING))),10) < 9 THEN true
        WHEN MOD(ABS(FARM_FINGERPRINT(CAST(tenMinute AS STRING))),10) = 9 THEN false
      END AS dataframe
    from (
      SELECT
      TIMESTAMP_SUB(TIMESTAMP_TRUNC(created, MINUTE), INTERVAL mod( extract(minute from created),10) MINUTE) tenMinute,
      roomId,
      avg(temperature) temp,
      avg(azimuth) azimuth,
      avg(zenith) zenith,
      avg(clouds) clouds,
      FROM `sensors.sensor`
      where zenith < 90 and roomId is not null and created > TIMESTAMP "2020-09-01 00:00:00+00"
      group by roomId, tenMinute
      )
  )
  where roomId = '%s'
  and dataframe=true
  WINDOW rollingWindow AS
  (partition by roomId ORDER BY time_diff RANGE BETWEEN 120 PRECEDING AND CURRENT ROW)

  )))
SELECT
threshold, recall, false_positive_rate,
ABS(recall - 0.7) AS from_desired_recall
FROM roc
ORDER BY from_desired_recall ASC
LIMIT 1


