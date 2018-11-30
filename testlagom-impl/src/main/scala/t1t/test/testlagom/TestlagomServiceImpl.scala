package t1t.test.testlagom

import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import t1t.test.testlagom

/**
  * Implementation of the TestlagomService.
  */
class TestlagomServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends TestlagomService {

  override def getKey(user: String) = ServiceCall { _ =>
    // Look up the TestLagom entity for the given ID.
    val ref = persistentEntityRegistry.refFor[TestlagomEntity](user)

    // Ask the entity the Hello command.
    ref.ask(GetKey(user))
  }

  override def storeKey(user: String) = ServiceCall { request =>
    // Look up the TestLagom entity for the given ID.
    val ref = persistentEntityRegistry.refFor[TestlagomEntity](user)

    // Tell the entity to use the greeting message specified.
    ref.ask(StoreKey(request.key))
  }


  override def sshTopic(): Topic[SshKeyChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(TestlagomEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(sshEvent: EventStreamElement[TestlagomEvent]): SshKeyChanged = {
    sshEvent.event match {
        // als de event key changed getriggerd is, dan updaten van entity met nieuwe sshkey
      case KeyChanged(key) => testlagom.SshKeyChanged(sshEvent.entityId, key)
    }
  }
}
