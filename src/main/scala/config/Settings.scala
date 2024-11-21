package config

object Settings {
  val KAFKA_BOOTSTRAP_SERVERS = "localhost:9092" // Kafka server address
  val kafkaGroupId = "task-notification-group" // Unique group ID for the Kafka consumer
}
