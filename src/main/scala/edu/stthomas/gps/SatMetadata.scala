package edu.stthomas.gps

case class SatMetadata(x: Array[Double], y: Array[Double], satHeight: Float, satLongitude: Float,
                       satSweep: String, date: Int) {}