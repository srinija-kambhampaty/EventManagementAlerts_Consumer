import akka.actor.Actor
import model.Task

class TaskProcessor extends Actor {
  override def receive: Receive = {
    case task: Task =>
      // Convert the status to lowercase to handle case insensitivity
      task.status.toLowerCase match {
        case "pending" =>
          println(s"Task ${task.taskName} is pending. Please follow up.")

        case "in progress" =>
          println(s"Task ${task.taskName} is in progress. Keep up the work!")

        case "completed" =>
          println(s"Task ${task.taskName} has been completed. Well done!")

        case _ =>
          println(s"Unknown task status: ${task.status}")
      }
  }
}
