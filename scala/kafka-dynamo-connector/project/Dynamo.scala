import sbt.Keys._
import sbt._
import com.localytics.sbt.dynamodb.DynamoDBLocalKeys._

object Dynamo {
  val settings = Seq(
    startDynamoDBLocal := startDynamoDBLocal.dependsOn(compile in Test).value,
    test in Test := (test in Test).dependsOn(startDynamoDBLocal).value,
    testOnly in Test := (testOnly in Test).dependsOn(startDynamoDBLocal).evaluated,
    testOptions in Test += dynamoDBLocalTestCleanup.value
  )
}
