package io.pathfinder.routing

import io.pathfinder.models.{Transport, Commodity}
import play.api.libs.json._

sealed abstract class Action(val name: String) {
    def latitude: Double
    def longitude: Double
}

object Action {

    case class Start(
        latitude: Double,
        longitude: Double
    ) extends Action("Start") {
        def this(v: Transport) = this(
            v.latitude,
            v.longitude
        )
    }

    sealed abstract class CommodityAction(name: String) extends Action(name) {
        def commodity: Commodity
    }

    case class PickUp(
        latitude: Double,
        longitude: Double,
        commodity: Commodity
    ) extends CommodityAction("PickUp") {
        def this(com: Commodity) = this(
            com.startLatitude,
            com.startLongitude,
            com
        )
    }

    case class DropOff(
        latitude: Double,
        longitude: Double,
        commodity: Commodity
    ) extends CommodityAction("DropOff") {
        def this(com: Commodity) = this(
            com.endLatitude,
            com.endLongitude,
            com
        )
    }

    implicit val writes = Writes[Action]{
        a =>
        val json = Json.obj(
            "action" -> a.name,
            "latitude" -> a.latitude,
            "longitude" -> a.longitude
        )
        a match {
            case coma : CommodityAction => json++Json.obj("commodity"->Commodity.format.writes(coma.commodity))
            case x => json
        }
    }
}
