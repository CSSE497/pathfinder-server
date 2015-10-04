package io.pathfinder.routing

import io.pathfinder.models.Commodity
import play.api.libs.json.{Writes, Json, JsPath, Format}

sealed abstract class Action(val name: String) {
    def latitude: Double
    def longitude: Double
    def commodity: Long
}

object Action {

    case class PickUp(
        latitude: Double,
        longitude: Double,
        commodity: Long
    ) extends Action("pickup") {
        def this(com: Commodity) = this(
            com.startLatitude,
            com.startLongitude,
            com.id
        )
    }

    case class DropOff(
        latitude: Double,
        longitude: Double,
        commodity: Long
    ) extends Action("dropoff") {
        def this(com: Commodity) = this(
            com.endLatitude,
            com.endLongitude,
            com.id
        )
    }

    implicit val format: Format[Action] = Format(
        for {
            name <- (JsPath \ "action").read[String]
            lat <- (JsPath \ "latitude").read[Double]
            lng <- (JsPath \ "longitude").read[Double]
            com <- (JsPath \ "commodity").read[Long]
        } yield {
            name match {
                case "pickup" => PickUp(lat, lng, com)
                case "dropoff" => DropOff(lat, lng, com)
            }
        }, Writes[Action](
            a => Json.obj(
                "action" -> a.name,
                "latitude" -> a.latitude,
                "longitude" -> a.longitude,
                "commodity" -> a.commodity
            )
        )
    )
}
