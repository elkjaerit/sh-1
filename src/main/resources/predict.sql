SELECT
  *
FROM
  ML.PREDICT (MODEL `%s`,
    (
    SELECT
      %s as cloud_bucket, %s as azimuth_bucket, %s as zenith_bucket
    )
  )
