package io.pathfinder.routing

import io.pathfinder.models.{Vehicle, Commodity}
import play.api.libs.json.{Writes, Json, JsPath, Format}

sealed abstract class Action(val name: String) {
    def latitude: Double
    def longitude: Double
}

object Action {

    case class Start(
        latitude: Double,
        longitude: Double
    ) extends Action("start") {
        def this(v: Vehicle) = this(
            v.latitude,
            v.longitude
        )
    }

    sealed abstract class CommodityAction(name: String) extends Action(name) {
        def commodityId: Long
    }

    case class PickUp(
        latitude: Double,
        longitude: Double,
        commodityId: Long
    ) extends CommodityAction("pickup") {
        def this(com: Commodity) = this(
            com.startLatitude,
            com.startLongitude,
            com.id
        )
    }

    case class DropOff(
        latitude: Double,
        longitude: Double,
        commodityId: Long
    ) extends CommodityAction("dropoff") {
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
            com <- (JsPath \ "commodityId").read[Long] if name == "pickup" || name == "dropoff"
        } yield {
            name match {
                case "pickup" => PickUp(lat, lng, com)
                case "dropoff" => DropOff(lat, lng, com)
                case "start" => Start(lat, lng)
            }
        }, Writes[Action]{
            a =>
            val json = Json.obj(
                "action" -> a.name,
                "latitude" -> a.latitude,
                "longitude" -> a.longitude
            )
            a match {
                case coma : CommodityAction => json++Json.obj("commodityId"->coma.commodityId)
                case x => json
            }
        }
    )
}
