import akka.actor.{Actor, ActorRef}
import model.Task
import play.api.libs.json.Json
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecords, ConsumerRecord}
import java.util.Properties
import org.slf4j.LoggerFactory

class NotificationHandler(taskProcessor: ActorRef) extends Actor {

  // Set up the logger
  private val logger = LoggerFactory.getLogger("TaskAlertLogger")

  // Kafka Consumer configuration
  private val consumerConfig = new Properties()
  consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.Settings.kafkaBootstrapServers)
  consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, config.Settings.kafkaGroupId)
  consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  val kafkaConsumer = new KafkaConsumer[String, String](consumerConfig)
  kafkaConsumer.subscribe(java.util.Collections.singletonList("taskcreation_alert"))

  override def receive: Receive = {
    case "start-consumer" => consumeMessages()

    case message: String =>
      // Deserialize the message into a Task object
      Json.parse(message).validate[Task].asOpt match {
        case Some(task) =>
          // Log the task details to the log file
          println(s"inside the notificationhandler message $task")
          logger.info(s"Received task alert: $task")
          taskProcessor ! task
        case None =>
          logger.error(s"Failed to parse message: $message")
      }
  }

  def consumeMessages(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          val records: ConsumerRecords[String, String] = kafkaConsumer.poll(1000)
          records.forEach { record: ConsumerRecord[String, String] =>
            val taskJson = record.value()
            self ! taskJson // Send the JSON string to the actor for deserialization
          }
        }
      }
    }).start()
  }
}
