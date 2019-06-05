name := "zeppelin-jar-loader"

version := "0.1"

val scalaVer = "2.11.12"

lazy val testScalafmt = taskKey[Unit]("testScalafmt")

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := scalaVer,
  resolvers += DefaultMavenRepository,
  libraryDependencies ++= Seq(
    // Common test dependencies
    "org.apache.zeppelin" %% "zeppelin-spark" % "0.7.3",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  ),
  // disable parallel test execution to avoid SparkSession conflicts
  parallelExecution in Test := false
)

def assemblySettings = Seq(
  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", xs @ _*) => MergeStrategy.last
    case PathList("META-INF", xs @ _*)      => MergeStrategy.discard
    case x if x.endsWith("io.netty.versions.properties") =>
      MergeStrategy.discard
    case x => MergeStrategy.first
  }
)

lazy val root = (project in file(".")).settings(
  commonSettings,
  assemblySettings,
  libraryDependencies ++= Seq(
    )
)
