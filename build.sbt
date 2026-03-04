Global / onChangedBuildSource := ReloadOnSourceChanges
Global / onLoad               := (Global / onLoad).value.andThen { s =>
  println(s"version: ${(ThisBuild / version).value}")
  s
}

inThisBuild(
  List(
    organization := "io.github.ssstlis",
    sbtPlugin    := true,
    version      := {
      dynverGitDescribeOutput.value match {
        case Some(out) if out.isSnapshot =>
          val tag = out.ref.value.stripPrefix("v")
          s"$tag+${out.commitSuffix.distance}-${out.commitSuffix.sha}-SNAPSHOT"
        case Some(out) =>
          out.ref.value.stripPrefix("v")
        case None =>
          "0.0.1-SNAPSHOT"
      }
    },
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalacOptions += "-Ywarn-unused-import"
  )
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "sbt-local-deploy",
    addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.7"),
    addSbtPlugin("com.github.sbt" % "sbt-git"             % "2.1.0"),
    addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt"),
    addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
  )
