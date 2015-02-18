name := "bachelorClient"

version := "1.0"

scalaVersion := "2.11.5"

libraryDependencies += "pdeboer" % "pplib_2.11" % "0.1-SNAPSHOT"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.6"

libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.3.6"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.5"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"

libraryDependencies += "com.typesafe" % "config" % "1.2.0"

// Scala 2.10, 2.11
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"       % "2.2.2",
  "com.h2database"  %  "h2"                % "1.4.184",
  "ch.qos.logback"  %  "logback-classic"   % "1.1.2",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.2.2"
)


resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)