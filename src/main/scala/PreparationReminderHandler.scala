import akka.actor.Actor
import model.Task
import play.api.libs.json.Json
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords, ConsumerRecord}
import java.util.Properties
import org.slf4j.LoggerFactory

class PreparationReminderHandler extends Actor {
  // Set up a separate logger for preparation reminders
  private val logger = LoggerFactory.getLogger("PreparationReminderLogger")

  // Kafka Consumer configuration
  private val consumerConfig2 = new Properties()
  consumerConfig2.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.Settings.kafkaBootstrapServers)
  consumerConfig2.put(ConsumerConfig.GROUP_ID_CONFIG, "preparation-reminder-group")
  consumerConfig2.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerConfig2.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  val kafkaConsumer = new KafkaConsumer[String, String](consumerConfig2)
  kafkaConsumer.subscribe(java.util.Collections.singletonList("preparation_reminders"))

  override def receive: Receive = {
    case "start-consumer" => consumeMessages()

    case message: String =>
      println(s"Preparation Remainder for $message")
      logger.info(s"Raw message: $message") // Add this line
      Json.parse(message).validate[Task].asOpt match {
        case Some(task) =>
          logger.info(s"Preparation Reminder: Task ${task.taskName} - ${task.description}")
        case None =>
          logger.error(s"Failed to parse message: $message")
      }
  }

  def consumeMessages(): Unit = {
    new Thread(() => {
      while (true) {
        val records: ConsumerRecords[String, String] = kafkaConsumer.poll(1000)
        records.forEach { record: ConsumerRecord[String, String] =>
          val taskJson = record.value()
          self ! taskJson
        }
      }
    }).start()
  }
}
