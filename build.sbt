val scala3Version = "3.3.1"
val pekkoVersion = "1.0.2"
val zioVersion = "2.0.19"
val zioHttpVersion = "3.0.0-RC2" 
val circeVersion = "0.14.6"

lazy val root = project
  .in(file("."))
  .settings(
    name := "vortex-mvp",
    version := "0.3.0-API",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-cluster-typed" % pekkoVersion,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "com.uber" % "h3" % "4.1.1",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.11"
    ),
    scalacOptions ++= Seq("-encoding", "utf8", "-feature", "-Werror")
  )
