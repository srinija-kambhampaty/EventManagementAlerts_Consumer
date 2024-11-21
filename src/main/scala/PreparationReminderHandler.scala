import akka.actor.Actor
import model.Task
import play.api.libs.json.Json
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords, ConsumerRecord}
import java.util.Properties
import org.slf4j.LoggerFactory
import config.Settings.KAFKA_BOOTSTRAP_SERVERS

class PreparationReminderHandler extends Actor {
  // Set up a separate logger for preparation reminders
  private val logger = LoggerFactory.getLogger("PreparationReminderLogger")

  // Kafka Consumer configuration
  private val consumerConfig2 = new Properties()
  consumerConfig2.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS)
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
          // Descriptive logging for the preparation reminder
          logger.info("\n\n==================== Preparation Reminder ====================")
          logger.info(s"Subject: Reminder for Task Preparation - '${task.taskName}'")
          logger.info("------------------------------------------------------------")
          logger.info(s"Dear Team,")
          logger.info("")
          logger.info(s"This is a gentle reminder to prepare for the upcoming task '${task.taskName}'.")
          logger.info("Task Details:")
          logger.info(s"  - Description: '${task.description}'")
          logger.info(s"  - Assigned Date: '${task.assignedDate}'")
          logger.info(s"  - Due Date: '${task.dueDate}'")
          logger.info(s"  - Current Status: '${task.status}'")
          logger.info("")
          logger.info("Please ensure all necessary preparations are completed on time.")
          logger.info("------------------------------------------------------------")
          logger.info("End of Reminder")
          logger.info("============================================================\n\n")
        case None =>
          // Logging for message parsing errors
          logger.error("\n\n======================= Parsing Error =======================")
          logger.error(s"Failed to parse message: $message")
          logger.error("============================================================\n\n")
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
