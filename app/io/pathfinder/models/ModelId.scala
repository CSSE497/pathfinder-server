package io.pathfinder.models

import io.pathfinder.websockets.ModelTypes
import io.pathfinder.websockets.ModelTypes.ModelType
import play.api.libs.json.{Reads, JsString, JsNumber, JsResult, JsValue, Writes, Json, Format}

object ModelId {

    sealed abstract class LongId extends ModelId {
        override type Key = Long
    }

    sealed abstract class StringId extends ModelId {
        override type Key = String
    }

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

    case class CommodityId(id: Long) extends LongId {
        override type Id = CommodityId
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
    case class ClusterPath(path: String) extends StringId {
        override type Id = ClusterPath
        override def modelType = ModelTypes.Cluster
        override def id = path
        override def withAppId(appId: String): Option[ClusterPath] =
            Cluster.addAppToPath(path, appId).map(ClusterPath.apply)
        override def withoutAppId: ClusterPath =
            ClusterPath(Cluster.removeAppFromPath(path))
    }

    object VehicleId {
        val format: Format[VehicleId] = Format(
            Reads.JsNumberReads.map(num => VehicleId.apply(num.value.toLong)),
            Writes {
                case VehicleId(id) => JsNumber(id)
            }
        )
    }

    case class VehicleId(id: Long) extends LongId {
        override type Id = VehicleId
        override def modelType = ModelTypes.Vehicle
    }
}

sealed abstract class ModelId {
    protected type Id <: ModelId
    type Key
    def modelType: ModelType
    def id: Key
    def withAppId(app: String): Option[Id] = Some(this.asInstanceOf[Id])
    def withoutAppId: Id = this.asInstanceOf[Id]
}
