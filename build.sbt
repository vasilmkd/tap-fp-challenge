name := "tap-fp-challenge"

version := "0.0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.scalaz" %% "scalaz-zio" % "1.0-RC4"

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-language:higherKinds"
)
