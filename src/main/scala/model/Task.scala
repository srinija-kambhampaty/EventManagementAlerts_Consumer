package model

import play.api.libs.json._

case class Task(
                 id: Long,
                 eventId: Long,
                 teamId: Long,
                 taskName: String,
                 description: String,
                 assignedDate: String,
                 dueDate: String,
                 status: String,
                 totalTimeHours: Option[Double],
                 remainingHours: Option[Double],
                 completedHours: Option[Double],
                 blockers: Option[String]
               )

object Task {
  implicit val taskFormat: Format[Task] = Json.format[Task]
}