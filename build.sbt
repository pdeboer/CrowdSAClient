name := "bachelorClient"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies += "pdeboer" % "pplib_2.11" % "0.1-SNAPSHOT"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.6"

libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.3.6"

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)