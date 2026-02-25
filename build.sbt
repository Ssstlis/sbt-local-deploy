Global / onChangedBuildSource := ReloadOnSourceChanges
Global / onLoad               := (Global / onLoad).value.andThen { s =>
  println(s"version: ${(ThisBuild / version).value}")
  s
}

ThisBuild / sbtPlugin := true

ThisBuild / organization     := "io.github.ssstlis"
ThisBuild / organizationName := "Ssstlis"
name                         := "sbt-local-deploy"

addSbtPlugin("com.github.sbt"   % "sbt-native-packager" % "1.11.7")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.2")

ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage    := Some(url("https://github.com/Ssstlis/sbt-local-deploy"))
ThisBuild / description := "Local SBT deployment util."
ThisBuild / developers := List(Developer("Ssstlis", "Ivan Aristov", "ssstlis@pm.me", url("https://github.com/ssstlis")))
ThisBuild / scmInfo    := Some(
  ScmInfo(url("https://github.com/Ssstlis/sbt-local-deploy"), "git@github.com:Ssstlis/sbt-local-deploy.git")
)

ThisBuild / version := {
  dynverGitDescribeOutput.value match {
    case Some(out) if out.isSnapshot =>
      val tag = out.ref.value.stripPrefix("v")
      s"$tag+${out.commitSuffix.distance}-${out.commitSuffix.sha}-SNAPSHOT"
    case Some(out) =>
      out.ref.value.stripPrefix("v")
    case None =>
      "0.0.1-SNAPSHOT"
  }
}

ThisBuild / versionScheme := Some("pvp")

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
