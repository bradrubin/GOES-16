package edu.stthomas.gps


import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.dia.core.SciSparkContext
import org.dia.tensors.AbstractTensor

object ColorHDFS {

  def main(args: Array[String]) {

    val sparkConf = new SparkConf().setAppName("Spark MaxCount")
    val sc = new SparkContext(sparkConf)

    if (args.length < 2) {
      println("Usage: ColorHDFS <input> <output>")
      System.exit(1)
    }

    val ssc = new SciSparkContext(sc)
    val inputRDD = ssc.netcdfRandomAccessDatasets(args(0),
      List("CMI_C01", "CMI_C02", "CMI_C03", "CMI_C13", "goes_imager_projection", "t", "time_bounds", "x", "x_image_bounds", "y", "y_image_bounds"), 15, NaNs = true)

    val red = "CMI_C02"
    val green = "CMI_C03"
    val blue = "CMI_C01"
    val ir = "CMI_C13"

    def writeToHDFS(rdd: RDD[(String, BufferedImage)], path: String, prefix: String): Unit = {
      rdd.foreach(f => {
        val output = new File("/tmp/" ++ prefix ++ f._1)
        ImageIO.write(f._2, "jpg", output)
        val conf = new Configuration()
        val fs = FileSystem.get(new URI(path), conf)
        FileUtil.copy(new File("/tmp/" + prefix ++ f._1), fs, new Path(path), true, conf)
      })
    }

    val outputRDD = inputRDD.map { ds =>
      val RGB = calcRGB(ds(red)(), ds(green)(), ds(blue)(), ds(ir)())

      val bi = new BufferedImage(2500, 1500, BufferedImage.TYPE_INT_RGB)
      bi.setRGB(0, 0, 2500, 1500, RGB, 0, 2500)
      (ds.datasetName.dropRight(3) ++ ".jpg", bi)
    }

    writeToHDFS(outputRDD, args(1), "RGB_")
  }

  def calcRGB(red: AbstractTensor, green: AbstractTensor, blue: AbstractTensor, ir: AbstractTensor): Array[Int] = {

    def normalize(in: Double) = if (in.isNaN) 0f else if (in > 1f) 1f else in.toFloat

    val gammaRed = red.map(math.sqrt)
    val gammaGreen = green.map(math.sqrt)
    val gammaBlue = blue.map(math.sqrt)
    val IR = ((ir - 90.0) / (313.0 - 90.0)).map(x => 1 - x)

    val trueGreen = gammaRed * 0.48358168 + gammaBlue * 0.45706946 + gammaGreen * 0.06038137

    val maxRed = gammaRed.data.zip(IR.data).map { case (a, b) => math.max(a, b) }
    val maxGreen = trueGreen.data.zip(IR.data).map { case (a, b) => math.max(a, b) }
    val maxBlue = gammaBlue.data.zip(IR.data).map { case (a, b) => math.max(a, b) }

    maxRed.zip(maxGreen).zip(maxBlue).map {
      case ((a, b), c) => new Color(normalize(a), normalize(b), normalize(c)).getRGB
    }
  }
}
