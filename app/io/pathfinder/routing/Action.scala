package io.pathfinder.routing

import io.pathfinder.models.Commodity
import play.api.libs.json.{Json,Format}

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

    implicit val format = Format(
        Json.reads[(String,Double,Double,Long)].map{
            case ("pickup",latitude,longitude,commodity)  => PickUp(latitude,longitude,commodity)
            case ("dropoff",latitude,longitude,commodity) => DropOff(latitude,longitude,commodity)
        },
        {
            case a: Action => Json.writes[(String,Double,Double,Long)].writes(
                a.name,
                a.latitude,
                a.longitude,
                a.commodity
            )
        }
    )
}
