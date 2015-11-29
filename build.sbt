name := "DigraphWeb"

version := "1.0"

lazy val `digraphweb` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

//libraryDependencies ++= Seq(jdbc, cache, ws, specs2 % Test)

libraryDependencies += "org.webjars" % "bootstrap" % "3.3.4"

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator
