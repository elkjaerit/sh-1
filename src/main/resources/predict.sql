SELECT
  *
FROM
  ML.PREDICT (MODEL `%s`,
    (
    SELECT
      %s as cloudBucket, %s as azimuth_bucket, %s as zenith_bucket
    )
  )
