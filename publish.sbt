ThisBuild / organizationName := "Ssstlis"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / homepage    := Some(url("https://github.com/Ssstlis/sbt-local-deploy"))
ThisBuild / description := "Local SBT deployment util."
ThisBuild / developers := List(Developer("Ssstlis", "Ivan Aristov", "ssstlis@pm.me", url("https://github.com/ssstlis")))
ThisBuild / scmInfo    := Some(
  ScmInfo(url("https://github.com/Ssstlis/sbt-local-deploy"), "git@github.com:Ssstlis/sbt-local-deploy.git")
)

ThisBuild / versionScheme := Some("pvp")
