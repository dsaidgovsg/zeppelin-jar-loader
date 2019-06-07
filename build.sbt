val nameVal = "zeppelin-jar-loader"
name := nameVal

val versionVal = "v0.2.0"
version := versionVal

val scalaVersionVal = "2.11.12"

lazy val testScalafmt = taskKey[Unit]("testScalafmt")

lazy val commonSettings = Seq(
  version := versionVal,
  scalaVersion := scalaVersionVal,
  resolvers += DefaultMavenRepository,
  libraryDependencies ++= Seq(
    // Common test dependencies
    "org.apache.zeppelin" %% "zeppelin-spark" % "0.7.3",
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
  assemblyJarName in assembly := f"${nameVal}-${versionVal}.jar",
)

lazy val root = (project in file(".")).settings(
  commonSettings,
  assemblySettings,
  libraryDependencies ++= Seq()
)
