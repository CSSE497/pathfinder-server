package io.pathfinder.models

import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.ModelTypes.ModelType
import play.api.libs.json.{Reads, JsString, JsNumber, JsResult, JsValue, Writes, Json, Format}

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
            Reads.JsNumberReads.map(num => CommodityId(num.value.longValue())),
            Writes {
                case CommodityId(id) => JsNumber(id)
            }
        )
    }
    case class CommodityId(id: Long) extends ModelId {
        override def modelType = ModelTypes.Commodity
    }

    object ClusterPath {
        val format: Format[ClusterPath] = Format(
            Reads.JsStringReads.map(str => ClusterPath(str.value)),
            Writes {
                case ClusterPath(path) => JsString(path)
            }
        )
    }
    case class ClusterPath(path: String) extends ModelId {
        override def modelType = ModelTypes.Cluster
    }

    object VehicleId {
        val format: Format[VehicleId] = Format(
            Reads.JsNumberReads.map(num => VehicleId.apply(num.value.toLong)),
            Writes {
                case VehicleId(id) => JsNumber(id)
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
