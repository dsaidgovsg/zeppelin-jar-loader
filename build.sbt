val nameVal = "zeppelin-jar-loader"
name := nameVal

val versionVal = "v0.2.1"
version := versionVal

val scalaVersionVal = sys.env.get("SCALA_VERSION").getOrElse("2.11.12")
val scalaXYVersionVal = scalaVersionVal.split(raw"\.").take(2).mkString(".")

val zeppelinVersionVal = sys.env.get("ZEPPELIN_VERSION").getOrElse("0.8.2")

unmanagedBase := baseDirectory.value / "libs"

lazy val testScalafmt = taskKey[Unit]("testScalafmt")

lazy val commonSettings = Seq(
  version := versionVal,
  scalaVersion := scalaVersionVal,
  resolvers += DefaultMavenRepository,
  libraryDependencies ++= Seq(
    // Common test dependencies
    "org.apache.zeppelin" % "spark-interpreter" % zeppelinVersionVal,
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  ),
  // Disable parallel test execution to avoid SparkSession conflicts
  parallelExecution in Test := false
)

def assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("META-INF", xs @ _*)      => MergeStrategy.discard
    case x if x.endsWith("io.netty.versions.properties") =>
      MergeStrategy.discard
    case x => MergeStrategy.first
  },
  assemblyJarName in assembly := f"${nameVal}_${versionVal}_${scalaXYVersionVal}_zeppelin-${zeppelinVersionVal}.jar",
)

lazy val root = (project in file(".")).settings(
  commonSettings,
  assemblySettings,
  libraryDependencies ++= Seq()
)
