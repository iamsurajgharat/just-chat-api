name := """just-chat-api"""
organization := "com.surajgharat.justchat"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, AshScriptPlugin)
  .settings(
    scalacOptions += "-Wunused"
  )

inThisBuild(
  List(
    scalaVersion := "2.13.6",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

val AkkaVersion = "2.6.15"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.surajgharat.justchat.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.surajgharat.justchat.binders._"

import com.typesafe.sbt.packager.docker.DockerChmodType
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := DockerPermissionStrategy.CopyChown

Docker / maintainer := "mr.surajgharat2@gmail.com"
Docker / packageName := "just-chat-api"
Docker / version := sys.env.getOrElse("BUILD_NO", "0")
Docker / daemonUserUid := None
Docker / daemonUser := "daemon"
dockerExposedPorts := Seq(9000)
dockerBaseImage := "openjdk:8-jre-alpine"
dockerUpdateLatest := true
