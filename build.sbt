
Global / onChangedBuildSource := ReloadOnSourceChanges

sbtPlugin := true

organization := "io.github.ssstlis"
name         := "sbt-local-deploy"

addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.11.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.0")

ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage    := Some(url("https://github.com/Ssstlis/excel-sorter"))
ThisBuild / description := "Excel sorting and compare util."
ThisBuild / developers := List(Developer("Ssstlis", "Ivan Aristov", "ssstlis@pm.me", url("https://github.com/ssstlis")))
ThisBuild / scmInfo    := Some(
  ScmInfo(url("https://github.com/Ssstlis/excel-sorter"), "git@github.com:Ssstlis/excel-sorter.git")
)

ThisBuild / version := {
  val envVersion = sys.env.getOrElse("APP_VERSION", "-SNAPSHOT")
  if (envVersion.endsWith("SNAPSHOT")) {
    git.gitHeadCommit.value.getOrElse("").take(8) + envVersion
  } else envVersion
}
ThisBuild / versionScheme := Some("pvp")

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")