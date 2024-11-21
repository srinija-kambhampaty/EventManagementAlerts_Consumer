import akka.actor.Actor
import model.Task
import play.api.libs.json.Json
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords, ConsumerRecord}
import java.util.Properties
import org.slf4j.LoggerFactory
import config.Settings.KAFKA_BOOTSTRAP_SERVERS

class ProgressCheckInHandler extends Actor {
  private val logger = LoggerFactory.getLogger("ProgressCheckInLogger")

  // Kafka Consumer configuration
  private val consumerConfig = new Properties()
  consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS)
  consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "progress-check-in-group")
  consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  val kafkaConsumer = new KafkaConsumer[String, String](consumerConfig)
  kafkaConsumer.subscribe(java.util.Collections.singletonList("progress_check_in"))

  override def receive: Receive = {
    case "start-consumer" => consumeMessages()

    case message: String =>
      Json.parse(message).validate[Task].asOpt match {
        case Some(task) =>
          // Console output for visibility
          println(s"Please update these tasks: $task")

          // Descriptive logging for the progress check-in
          logger.info("\n\n===================== Progress Check-In =====================")
          logger.info(s"Subject: Task '${task.taskName}' In Progress - Status Update Required")
          logger.info("------------------------------------------------------------")
          logger.info(s"Dear Team,")
          logger.info("")
          logger.info(s"We noticed that the task '${task.taskName}' is currently in progress.")
          logger.info("Task Details:")
          logger.info(s"  - Description: '${task.description}'")
          logger.info(s"  - Assigned Date: '${task.assignedDate}'")
          logger.info(s"  - Due Date: '${task.dueDate}'")
          logger.info(s"  - Current Status: '${task.status}'")
          logger.info(s"  - Total Hours : '${task.totalTimeHours}'")
          logger.info(s"  - Pending Hours: '${task.remainingHours}'")
          logger.info(s"  - Completed Hours: '${task.completedHours}'")
          logger.info("")
          logger.info("Please update the status of this task at your earliest convenience.")
          logger.info("------------------------------------------------------------")
          logger.info("End of Check-In Reminder")
          logger.info("============================================================\n\n")

          // Raw Task Data for debugging
          logger.info("\n\n======================= Raw Task Data =======================")
          logger.info(s"$task")
          logger.info("============================================================\n\n")
        case None =>
          // Logging for parsing errors
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
