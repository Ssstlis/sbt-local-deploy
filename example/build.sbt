name         := "ldp-example"
organization := "io.github.ssstlis"
version      := "0.1.0"
scalaVersion := "2.13.16"

enablePlugins(LocalDeployPlugin)

libraryDependencies ++= Seq(
  "com.typesafe"   % "config"          % "1.4.3",
  "ch.qos.logback" % "logback-classic" % "1.5.18",
  "org.slf4j"      % "slf4j-api"       % "2.0.17"
)
