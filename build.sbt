inThisBuild(
  Seq(
    version := "0.1.0-SNAPSHOT",
    scalaVersion := "3.2.0"
  )
)

lazy val pb = project.settings(
  Compile / PB.targets := Seq(
    scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb",
    monix.grpc.codegen.GrpcCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
  ),
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
    "com.thesamet.scalapb" %% "scalapb-json4s" % "0.12.0",
    "com.linecorp.armeria" %% "armeria-scala" % "1.19.0",
    "com.linecorp.armeria" %% "armeria-scalapb" % "1.19.0",
    "com.linecorp.armeria" % "armeria-logback" % "1.19.0",
    "org.scalameta" %% "munit" % "1.0.0-M6" % Test
  ),
  Compile / javacOptions += "-parameters"
)
