name := """pathfinder-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayEbean, PlayJava)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  javaJdbc,
  cache,
  ws,
  filters,
  specs2 % Test,
  "org.avaje.ebeanorm" % "avaje-ebeanorm-api" % "3.1.1",
  "com.google.maps" % "google-maps-services" % "0.1.7",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalatestplus" %% "play" % "1.4.0-M3" % "test",
  "org.postgresql" % "postgresql" % "9.4-1203-jdbc42",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.0",
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.0"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q", "-a")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Docker configuration
packageName in Docker := "pathfinder-server"
version in Docker := "0.2.0"
maintainer in Docker := "Pathfinder Team"
dockerRepository := Some("beta.gcr.io/phonic-aquifer-105721")
dockerExposedPorts := Seq(9000, 9443)

scoverage.ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages := "<empty>;controllers\\..*Reverse.*;router\\..*Routes.*"
