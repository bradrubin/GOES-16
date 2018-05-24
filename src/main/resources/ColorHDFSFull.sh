#!/usr/bin/env bash

inputDir=day57
outputDir=colorOut
numPartitions=16
movieName=color
frameRate=3

hadoop fs -rm -r -skipTrash $outputDir/*
rm color/*

spark-submit \
      --class edu.stthomas.gps.ColorHDFS \
      --master yarn-cluster \
      --executor-memory 40G \
      --num-executors 16 \
      --executor-cores 11 \
      /home/brad/GOES-16/GOES-16.jar \
      $inputDir/ \
      $outputDir/ \
      $numPartitions

hadoop fs -get $outputDir/*.png color
rm $movieName.mp4

cat color/*.png | ffmpeg -f image2pipe -r $frameRate -i - -c:v libx264 -pix_fmt yuv420p $movieName.mp4