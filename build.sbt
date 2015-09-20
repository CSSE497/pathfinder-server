name := """pathfinder-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayEbean, PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  javaJdbc,
  cache,
  ws,
  specs2 % Test,
  "org.avaje.ebeanorm" % "avaje-ebeanorm-api" % "3.1.1",
  "com.google.maps" % "google-maps-services" % "0.1.7",
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q", "-a")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
