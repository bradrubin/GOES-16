package edu.stthomas.gps

import java.awt.Color
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO

import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.dia.core.{SciDataset, SciSparkContext}
import org.dia.tensors.AbstractTensor
import org.apache.hadoop.conf.Configuration

object ColorHDFS {

  def main(args: Array[String]) {

    if (args.length < 3) {
      println("Usage: ColorHDFS <inputDir> <outputDir> <numPartitions")
      System.exit(1)
    }
    val inputDir = args(0)
    val outputDir = args(1)
    val numPartitions = args(2).toInt

    val sparkConf = new SparkConf().setAppName("Spark Pseudocolor Satellite Images")
    val sc = new SparkContext(sparkConf)
    val ssc = new SciSparkContext(sc)
    val sqlContext = new org.apache.spark.sql.SQLContext(sc)
    import sqlContext.implicits._

    val red = "CMI_C02" // 0.64 um
    val veggie = "CMI_C03" // 0.86 um
    val blue = "CMI_C01" // 0.47 um
    val cleanIR = "CMI_C13" // 10.3 um

    def writeToHDFS(rdd: RDD[(String, BufferedImage)], path: String, prefix: String): Unit = {
      rdd.foreach(i => {
        val (fileName, image) = i
        val conf = new Configuration()
        val fs = FileSystem.get(new URI(path), conf)
        val out = fs.create(new Path(path + prefix + fileName))
        ImageIO.write(image, "png", out)
        out.close()
      })
    }

    val inputRDD = ssc.netcdfRandomAccessDatasets(inputDir,
      List("CMI_C01", "CMI_C02", "CMI_C03", "CMI_C13", "goes_imager_projection",
        "t", "x", "y"), numPartitions, NaNs = true)

    val outputRDD: RDD[(String, BufferedImage)] = inputRDD.map { ds =>
      val RGB = calcRGB(ds(red)(), ds(veggie)(), ds(blue)(), ds(cleanIR)())
      val shape = ds("CMI_C01").shape
      val x = shape(1)
      val y = shape(0)
      val bi = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB)
      bi.setRGB(0, 0, x, y, RGB, 0, x)
      (ds.datasetName.dropRight(3) + ".png", bi)
    }

    writeToHDFS(outputRDD, outputDir, "RGB_")

    val x: Array[Double] = inputRDD.map(_("x").data()).first
    val y: Array[Double] = inputRDD.map(_("y").data()).first
    val date: Int = inputRDD.map(_("t").data()(0)).first.toInt
    val first: SciDataset = inputRDD.first
    val satHeight: Float = first("goes_imager_projection").attributes("perspective_point_height").toFloat
    val satLongitude: Float = first("goes_imager_projection").attributes("longitude_of_projection_origin").toFloat
    val satSweep: String = first("goes_imager_projection").attributes("sweep_angle_axis")

    val satMetadata = SatMetadata(x, y, satHeight, satLongitude, satSweep, date)
    val dataRDD = sc.parallelize(Array(satMetadata))
    val dataDF = dataRDD.toDF
    dataDF.write.parquet(outputDir + "satMetadata.parquet")
  }

  def calcRGB(red: AbstractTensor, veggie: AbstractTensor, blue: AbstractTensor, cleanIR: AbstractTensor): Array[Int] = {

    def mask(in: Double): Float = in match {
      case x if x.isNaN || x < 0 => 0f
      case x if x > 1 => 1f
      case _ => in.toFloat
    }

    val gammaRed = red.map(math.sqrt)
    val gammaVeggie = veggie.map(math.sqrt)
    val gammaBlue = blue.map(math.sqrt)

    val minIR = 90.0
    val maxIR = 313.0
    val normIR = ((cleanIR - minIR) / (maxIR - minIR)).map(1 - _)

    val trueGreen = gammaRed * 0.48358168 + gammaBlue * 0.45706946 + gammaVeggie * 0.06038137

    val maxRed = gammaRed.data.zip(normIR.data).map { case (a, b) => math.max(a, b) }
    val maxGreen = trueGreen.data.zip(normIR.data).map { case (a, b) => math.max(a, b) }
    val maxBlue = gammaBlue.data.zip(normIR.data).map { case (a, b) => math.max(a, b) }

    maxRed.zip(maxGreen).zip(maxBlue).map {
      case ((a, b), c) => new Color(mask(a), mask(b), mask(c)).getRGB
    }
  }
}
