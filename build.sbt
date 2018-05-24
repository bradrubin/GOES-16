import sbt.Resolver

name := "GOES-16"

version := "0.1"

assemblyJarName in assembly := "GOES-16.jar"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

name := "GOES-16"

organization := "edu.stthomas.gps"

scalaVersion := "2.10.7"
val sparkVersion = "1.6.0"

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.file("Local", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0",
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.dia" %% "scispark" % "1.0")

assemblyMergeStrategy in assembly := {
  case x if x.startsWith("META-INF") => MergeStrategy.discard // Bumf
  case x if x.endsWith(".html") => MergeStrategy.discard // More bumf
  case x if x.contains("slf4j-api") => MergeStrategy.last
  case x if x.contains("org/cyberneko/html") => MergeStrategy.first
  case x if x.contains("SingleThreadModel.class") => MergeStrategy.first
  case x if x.contains("javax.servlet") => MergeStrategy.first
  case x if x.contains("org.eclipse") => MergeStrategy.first
  case x if x.contains("org.apache") => MergeStrategy.first
  case x if x.contains("org.slf4j") => MergeStrategy.first
  case x if x.endsWith("reference.conf") => MergeStrategy.concat
  case PathList("com", "esotericsoftware", xs@_ *) => MergeStrategy.last // For Log$Logger.class
  case x => MergeStrategy.first
}
