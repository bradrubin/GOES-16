#!/usr/bin/env bash
hadoop fs -rm -skipTrash day36out/*
spark-submit \
      --class edu.stthomas.gps.NDVIHDFS \
      --master yarn-cluster \
      --executor-memory 2G \
      --num-executors 15 \
      --executor-cores 11 \
      /home/brad/GOES-16/GOES-16.jar \
      day36/ \
      day36out/
