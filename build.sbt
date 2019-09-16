val nameVal = "zeppelin-jar-loader"
name := nameVal

val versionVal = "v0.2.1"
version := versionVal

val scalaVersionVal = sys.env.get("SCALA_VERSION").getOrElse("2.11.12")
val scalaXYVersionVal = scalaVersionVal.split(raw"\.").take(2).mkString(".")

unmanagedBase := baseDirectory.value / "libs"

lazy val testScalafmt = taskKey[Unit]("testScalafmt")

val zeppelinVersion = sys.env.get("ZEPPELIN_VERSION").getOrElse("0.8.1")
// sbt <command> -Dsbt.sourcemode=true to activate source build mode
lazy val sparkInterpreterRef = ProjectRef(uri("https://github.com/apache/zeppelin"), "zeppelin")
lazy val sparkInterpreterLib = "org.apache.zeppelin" % "spark-interpreter" % zeppelinVersion

lazy val commonSettings = Seq(
  version := versionVal,
  scalaVersion := scalaVersionVal,
  resolvers += DefaultMavenRepository,
  libraryDependencies ++= Seq(
    // Common test dependencies
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
  assemblyJarName in assembly := f"${nameVal}_${scalaXYVersionVal}-${versionVal}.jar",
)

lazy val root = (project in file("."))
  .sourceDependency(sparkInterpreterRef, sparkInterpreterLib)
  .settings(
    commonSettings,
    assemblySettings,
    libraryDependencies ++= Seq()
  )
