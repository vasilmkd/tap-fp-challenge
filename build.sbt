name := "tap-fp-challenge"

version := "0.0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.typelevel" %% "cats-effect" % "1.2.0"

scalacOptions ++= Seq(
  "-Ypartial-unification",
  "-language:higherKinds"
)
