// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.3")

addSbtPlugin("com.typesafe.sbt" %% "sbt-play-ebean" % "1.0.0")
resolvers += Classpaths.sbtPluginReleases
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.4")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0")
