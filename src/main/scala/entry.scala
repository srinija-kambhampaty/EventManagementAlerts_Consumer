import akka.actor.{ActorSystem, Props}

object Main extends App {
  // Initialize the Akka system
  val system = ActorSystem("TaskNotificationSystem")

  // Create the TaskProcessor actor
  val taskProcessor = system.actorOf(Props[TaskProcessor], "taskProcessor")

  // Create the NotificationHandler actor
  val notificationHandler = system.actorOf(Props(new NotificationHandler(taskProcessor)), "notificationHandler")

  // Create the PreparationReminderHandler actor
  val preparationReminderHandler = system.actorOf(Props(new PreparationReminderHandler()), "preparationReminderHandler")

  // Create the EventDayAlertHandler actor
  val eventDayAlertHandler = system.actorOf(Props(new EventDayAlertHandler()), "eventDayAlertHandler")

  // Create the ProgressCheckInHandler actor
  val progressCheckInHandler = system.actorOf(Props(new ProgressCheckInHandler()), "progressCheckInHandler")

  // Create the IssueAlertHandler actor
  val issueAlertHandler = system.actorOf(Props(new IssueAlertHandler()), "issueAlertHandler")


  // Start the Kafka consumer
  notificationHandler ! "start-consumer"
  preparationReminderHandler ! "start-consumer"
  eventDayAlertHandler ! "start-consumer"
  progressCheckInHandler ! "start-consumer"
  issueAlertHandler ! "start-consumer"
}
