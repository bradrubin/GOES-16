package edu.stthomas.gps

object NDVIHDFS {

  import java.io.File
  import java.net.URI

  import org.apache.hadoop.conf.Configuration
  import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
  import org.apache.spark.rdd.RDD
  import org.apache.spark.{SparkConf, SparkContext}
  import org.dia.core.{SciSparkContext, Variable}

  def main(args: Array[String]) {

    val sparkConf = new SparkConf().setAppName("Spark MaxCount")
    val sc = new SparkContext(sparkConf)

    if (args.length < 2) {
      println("Usage: NDVIHDFS <input> <output>")
      System.exit(1)
    }

    def calcNDVI(ch2: Variable, ch3: Variable): Variable = (ch3 - ch2) / (ch2 + ch3)

    def writeToHDFS(rdd: RDD[org.dia.core.SciDataset], path: String, prefix: String): Unit = {
      rdd.foreach(f => {
        f.writeToNetCDF(prefix ++ f.datasetName, "/tmp/")
        val conf = new Configuration()
        val fs = FileSystem.get(new URI(path), conf)
        FileUtil.copy(new File("/tmp/" + prefix ++ f.datasetName), fs, new Path(path), true, conf)
      })
    }

    val ssc = new SciSparkContext(sc)

    val inputRDD = ssc.netcdfRandomAccessDatasets(args(0),
      List("CMI_C02", "CMI_C03", "goes_imager_projection", "t", "time_bounds",
        "x", "x_image_bounds", "y", "y_image_bounds"), 15, NaNs = true)

    val outputRDD = inputRDD.map { ds =>
      ds("NDVI") =

        new Variable("NDVI", "float", calcNDVI(ds("CMI_C02"), ds("CMI_C03"))(),
          ds("CMI_C02").attributes, List(("y", 1500), ("x", 2500)))
    }

    writeToHDFS(outputRDD, args(1), "NDVI_")
    System.exit(0)
  }
}
