ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "main_app"
  )

val akkaVersion = "2.9.0-M2"
val akkaHttpVersion = "10.6.0-M1"
val requestScala = "0.8.0"
val logbackVersion = "1.4.14"
val json4sJacksonVersion =  "4.0.7"

libraryDependencies ++= Seq(
  // akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  // akka http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.lihaoyi" %% "requests" % requestScala,
  //  json
//  "org.json4s" %% "json4s-jackson" % json4sJacksonVersion,
  // logback - backend for slf4j
  "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime
)