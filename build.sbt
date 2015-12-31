name := "jack-sparrow-bikes"

version := "1.0"

scalaVersion := "2.11.7"

mainClass := Some("com.pirate.jacksparrow.Main")

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-ws" % "2.5.0-M1",
  "org.jsoup" % "jsoup" % "1.8.3"
)