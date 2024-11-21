import akka.actor.Actor
import model.Task
import play.api.libs.json.Json
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords, ConsumerRecord}
import java.util.Properties
import org.slf4j.LoggerFactory
import config.Settings.KAFKA_BOOTSTRAP_SERVERS

class EventDayAlertHandler extends Actor {
  private val logger = LoggerFactory.getLogger("EventDayAlertLogger")

  // Kafka Consumer configuration
  private val consumerConfig = new Properties()
  consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS)
  consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "event-day-alert-group")
  consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  val kafkaConsumer = new KafkaConsumer[String, String](consumerConfig)
  kafkaConsumer.subscribe(java.util.Collections.singletonList("event_day_alerts"))

  override def receive: Receive = {
    case "start-consumer" => consumeMessages()

    case message: String =>
      Json.parse(message).validate[Task].asOpt match {
        case Some(task) =>
          // Console output for quick visibility
          println(s"Received Event Day Alert: Task Pending for Today's Event - $task")

          // Descriptive logging messages
          logger.info("\n\n======================= Event Day Alert =======================")
          logger.info(s"Subject: Task '${task.taskName}' Pending for Today's Event")
          logger.info("------------------------------------------------------------")
          logger.info(s"Hello Event Manager,")
          logger.info("")
          logger.info(s"We would like to notify you that the task '${task.taskName}' is still pending for today's event.")
          logger.info(s"Task Details:")
          logger.info(s"  - Description: '${task.description}'")
          logger.info(s"  - Assigned Date: '${task.assignedDate}'")
          logger.info(s"  - Due Date: '${task.dueDate}'")
          logger.info(s"  - Current Status: '${task.status}'")
          logger.info("")
          logger.info("Please take immediate action to ensure the task is completed on time.")
          logger.info("------------------------------------------------------------")
          logger.info("End of Notification")
          logger.info("============================================================\n\n")

          // Raw Task Data for debugging
          logger.info("\n\n======================= Raw Task Data =======================")
          logger.info(s"$task")
          logger.info("============================================================\n\n")
        case None =>
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
