package t1t.test.testlagom

import java.time.LocalDateTime

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json.{Format, Json}

import scala.collection.immutable.Seq

class TestlagomEntity extends PersistentEntity {

  override type Command = TestlagomCommand[_]
  override type Event = TestlagomEvent
  override type State = TestlagomState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: TestlagomState = TestlagomState("", LocalDateTime.now.toString)

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    case TestlagomState(key, _) => Actions().onCommand[StoreKey, Done] {

      // Command handler for the UseGreetingMessage command
      case (StoreKey(newKey), ctx, state) =>
        // In response to this command, we want to first persist it as a
        // GreetingMessageChanged event
        ctx.thenPersist(
          KeyChanged(newKey)
        ) { _ =>
          // Then once the event is successfully persisted, we respond with done.
          ctx.reply(Done)
        }

    }.onReadOnlyCommand[GetKey, String] {

      // Command handler for the Hello command
      case (GetKey(user), ctx, state) =>
        // Reply with a message built from the current message, and the name of
        // the person we're meant to say hello to.
        ctx.reply(s"$key, $user!")

    }.onEvent {

      // Event handler for the GreetingMessageChanged event
      case (KeyChanged(newKey), state) =>
        // We simply update the current state to use the greeting message from
        // the event.
        TestlagomState(newKey, LocalDateTime.now().toString)

    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class TestlagomState(key: String, timestamp: String)

object TestlagomState {
  /**
    * Format for the hello state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the entity gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[TestlagomState] = Json.format
}

/**
  * This interface defines all the events that the TestlagomEntity supports.
  */
sealed trait TestlagomEvent extends AggregateEvent[TestlagomEvent] {
  def aggregateTag = TestlagomEvent.Tag
}

object TestlagomEvent {
  val Tag = AggregateEventTag[TestlagomEvent]
}

/**
  * An event that represents a change in greeting message.
  */
case class KeyChanged(key: String) extends TestlagomEvent

object KeyChanged {

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[KeyChanged] = Json.format
}

/**
  * This interface defines all the commands that the TestlagomEntity supports.
  */
sealed trait TestlagomCommand[R] extends ReplyType[R]

/**
  * A command to switch the greeting message.
  *
  * It has a reply type of [[Done]], which is sent back to the caller
  * when all the events emitted by this command are successfully persisted.
  */
case class StoreKey(key: String) extends TestlagomCommand[Done]

object StoreKey {

  /**
    * Format for the use greeting message command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[StoreKey] = Json.format
}

/**
  * A command to say hello to someone using the current greeting message.
  *
  * The reply type is String, and will contain the message to say to that
  * person.
  */
case class GetKey(user: String) extends TestlagomCommand[String]

object GetKey {

  /**
    * Format for the hello command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[GetKey] = Json.format
}

/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object TestlagomSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[StoreKey],
    JsonSerializer[GetKey],
    JsonSerializer[KeyChanged],
    JsonSerializer[TestlagomState]
  )
}
