package t1t.test.testlagom

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object TestlagomService  {
  val TOPIC_NAME = "sshkeys"
}

/**
  * The TestLagom service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the TestlagomService.
  */
trait TestlagomService extends Service {

  /**
    * Example: curl http://localhost:9000/api/hello/Alice
    */
  def getKey(user: String): ServiceCall[NotUsed, String]

  /**
    * Example: curl -H "Content-Type: application/json" -X POST -d '{"message":
    * "Hi"}' http://localhost:9000/api/hello/Alice
    */
  def storeKey(user: String): ServiceCall[SshKey, Done]


  /**
    * This gets published to Kafka.
    */
  def sshTopic(): Topic[SshKeyChanged]

  override final def descriptor = {
    import Service._
    // @formatter:off
    named("testlagom")
      .withCalls(
        pathCall("/api/get/:user", getKey _),
        pathCall("/api/store/:user", storeKey _)
      )
      .withTopics(
        topic(TestlagomService.TOPIC_NAME, sshTopic)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[SshKeyChanged](_.user)
          )
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}

/**
  * The greeting message class.
  */
case class SshKey(key: String)

object SshKey {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[SshKey] = Json.format[SshKey]
}



/**
  * The greeting message class used by the topic stream.
  * Different than [[GreetingMessage]], this message includes the name (id).
  */
case class SshKeyChanged(user: String, key: String)

object SshKeyChanged {
  /**
    * Format for converting greeting messages to and from JSON.
    *
    * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
    */
  implicit val format: Format[SshKeyChanged] = Json.format[SshKeyChanged]
}
