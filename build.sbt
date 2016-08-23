name := "twitter-sentiment-analysis"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "clojars" at "https://clojars.org/repo"
resolvers += "conjars" at "http://conjars.org/repo"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.8",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.8",
  "ch.megard" %% "akka-http-cors" % "0.1.4",
  "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.3.0",
  "org.apache.spark" %% "spark-core" % "2.0.0",
  "org.elasticsearch" %% "elasticsearch-spark" % "2.3.0",
  "joda-time" % "joda-time" % "2.9.4",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" artifacts (Artifact("stanford-corenlp", "models"), Artifact("stanford-corenlp")),
  "com.typesafe" % "config" % "1.3.0"
)