import Dependencies._

ThisBuild / scalaVersion := "2.12.13"
ThisBuild / version := "1.0.0-dev"
ThisBuild / organization := "com.serli"
ThisBuild / organizationName := "serli"

lazy val playJsonVersion = "2.9.2"
lazy val AkkaVersion = "2.6.17"
lazy val AkkaHttpVersion = "10.2.7"
lazy val excludesJackson = Seq(
  ExclusionRule(organization = "com.fasterxml.jackson.core"),
  ExclusionRule(organization = "com.fasterxml.jackson.datatype"),
  ExclusionRule(organization = "com.fasterxml.jackson.dataformat")
)

lazy val root = (project in file("."))
  .settings(
    name := "wasm-ss",
    libraryDependencies ++= Seq(
      "org.wasmer" % "wasmer-java" % "0.3.0" from "https://github.com/wasmerio/wasmer-java/releases/download/0.3.0/wasmer-jni-amd64-darwin-0.3.0.jar",
      "com.typesafe.akka"      %% "akka-actor-typed"       % AkkaVersion,
      "com.typesafe.akka"      %% "akka-actor"             % AkkaVersion,
      "com.typesafe.akka"      %% "akka-stream"            % AkkaVersion,
      "com.typesafe.akka"      %% "akka-http"              % AkkaHttpVersion,
      "ch.qos.logback"          % "logback-classic"        % "1.2.3",
      "com.typesafe.play"      %% "play-json"              % playJsonVersion,
      scalaTest                 % Test
    )
  )

scalacOptions ++= Seq(
  "-feature",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:existentials",
  "-language:postfixOps"
)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

// the settings for the example app

Compile / mainClass := Some("com.serli.quickies.wasm.Wasm")
reStart / mainClass := Some("com.serli.quickies.wasm.Wasm")
