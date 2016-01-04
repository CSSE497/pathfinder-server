package io.pathfinder.models

import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.ModelTypes.ModelType
import play.api.libs.json.{JsString, JsNumber, JsResult, JsValue, Writes, Json, Format}

object ModelId {

    def read(model: ModelType, jsonId: JsValue): JsResult[ModelId] = model match {
        case ModelTypes.Commodity => CommodityId.format.reads(jsonId)
        case ModelTypes.Cluster => ClusterPath.format.reads(jsonId)
        case ModelTypes.Vehicle => VehicleId.format.reads(jsonId)
    }

    def write(mId: ModelId): JsValue = mId match {
        case CommodityId(id) => JsNumber(id)
        case ClusterPath(path) => JsString(path)
        case VehicleId(id) => JsNumber(id)
    }

    object CommodityId {
        val format: Format[CommodityId] = Format(
            Json.reads[Long].map(CommodityId.apply),
            Writes {
                case CommodityId(id) => Json.writes[Long].writes(id)
            }
        )
    }
    case class CommodityId(id: Long) extends ModelId {
        override def modelType = ModelTypes.Commodity
    }

    object ClusterPath {
        val format: Format[ClusterPath] = Format(
            Json.reads[String].map(ClusterPath.apply),
            Writes {
                case ClusterPath(path) => Json.writes[String].writes(path)
            }
        )
    }
    case class ClusterPath(path: String) extends ModelId {
        override def modelType = ModelTypes.Cluster
    }

    object VehicleId {
        val format: Format[VehicleId] = Format(
            Json.reads[Long].map(VehicleId.apply),
            Writes {
                case VehicleId(id) => Json.writes[Long].writes(id)
            }
        )
    }
    case class VehicleId(id: Long) extends ModelId {
        override def modelType = ModelTypes.Vehicle
    }
}

sealed abstract class ModelId {
    def modelType: ModelType
}
