#!/usr/bin/env bash
hadoop fs -rm -skipTrash colorOut/*
rm ~/colorOut/*

spark-submit \
      --class edu.stthomas.gps.ColorHDFS \
      --master yarn-cluster \
      --executor-memory 8G \
      --num-executors 15 \
      --executor-cores 11 \
      /home/brad/GOES-16/GOES-16.jar \
      day36/ \
      colorOut/ \
      15

hadoop fs -get colorOut/*.jpg ~/colorOut
rm color.mp4

cat ~/colorOut/*.png | ffmpeg -f image2pipe -r 10 -i - -c:v libx264 -pix_fmt yuv420p color.mp4
