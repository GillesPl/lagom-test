package t1t.test.user.api

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}
import play.api.libs.json.{Format, Json}

object UserService  {
  val TOPIC_NAME = "users"
}

trait UserService extends Service {
  def createUser: ServiceCall[CreateUser, User]
  def getUser(userId: UUID): ServiceCall[NotUsed, User]
  def getUsers: ServiceCall[NotUsed, Seq[User]]


  override final def descriptor = {
    import Service._
    // @formatter:off
    named("user")
      .withCalls(
        pathCall("/api/user", createUser),
        pathCall("/api/user/:id", getUser _),
        pathCall("/api/users", getUsers)
      )
      .withAutoAcl(true)
    // @formatter:on
  }
}


case class User(id: UUID, name: String)

object User {
  implicit val format: Format[User] = Json.format
}

case class CreateUser(name: String)

object CreateUser {
  implicit val format: Format[CreateUser] = Json.format
}


// TODO add KAFKA user added