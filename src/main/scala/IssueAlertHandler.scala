import akka.actor.Actor
import model.Task
import play.api.libs.json.Json
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords, ConsumerRecord}
import java.util.Properties
import org.slf4j.LoggerFactory
import config.Settings.KAFKA_BOOTSTRAP_SERVERS

class IssueAlertHandler extends Actor {
  private val logger = LoggerFactory.getLogger("IssueAlertLogger")

  // Kafka Consumer configuration
  private val consumerConfig = new Properties()
  consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS)
  consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "issue-alert-group")
  consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  val kafkaConsumer = new KafkaConsumer[String, String](consumerConfig)
  kafkaConsumer.subscribe(java.util.Collections.singletonList("issue_alerts"))

  override def receive: Receive = {
    case "start-consumer" => consumeMessages()

    case message: String =>
      Json.parse(message).validate[Task].asOpt match {
        case Some(task) =>
          // Descriptive logging messages for the issue alert
          logger.info("\n\n======================== Issue Alert =========================")
          logger.info(s"Subject: Immediate Intervention Required for Task '${task.taskName}'")
          logger.info("------------------------------------------------------------")
          logger.info(s"Dear Event Manager,")
          logger.info("")
          logger.info(s"We have detected an issue with the task '${task.taskName}'.")
          logger.info("Details of the Blocker:")
          logger.info(s"  - Blocker Description: ${task.blockers.getOrElse("No details provided")}")
          logger.info("")
          logger.info("Immediate action is required to address this issue and ensure the task can proceed as planned.")
          logger.info("------------------------------------------------------------")
          logger.info("Task Details:")
          logger.info(s"  - Task Name: '${task.taskName}'")
          logger.info(s"  - Description: '${task.description}'")
          logger.info(s"  - Assigned Date: '${task.assignedDate}'")
          logger.info(s"  - Due Date: '${task.dueDate}'")
          logger.info(s"  - Current Status: '${task.status}'")
          logger.info("------------------------------------------------------------")
          logger.info("End of Notification")
          logger.info("============================================================\n\n")

          // Raw Task Data for debugging
          logger.info("\n\n======================= Raw Task Data =======================")
          logger.info(s"$task")
          logger.info("============================================================\n\n")
        case None =>
          // Logging the error for message parsing issues
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
