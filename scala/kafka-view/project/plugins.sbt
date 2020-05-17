logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.bintrayRepo("kamon-io", "sbt-plugins")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.2.0")
addSbtPlugin("com.itv" % "sbt-scalapact" % "2.3.2")
addSbtPlugin("io.kamon" % "sbt-aspectj-runner" % "1.1.1")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")
//addSbtPlugin("com.localytics" % "sbt-dynamodb" % "2.0.3")

