package io.pathfinder.routing

import io.pathfinder.models.{Cluster, Commodity, Vehicle}
import io.pathfinder.websockets.Events
import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.collection.mutable

class Router(cluster: Cluster) {

    // these should probably be thread safe eventually
    private val vehicleRoutes = mutable.Map[Vehicle,Route]
    private val commodityRoutes = mutable.Map[Commodity,Route]

    private def deleteVehicle(v: Vehicle): Unit ={

    }

    private def addVehicle(v: Vehicle): Unit = {

    }

    private def updateVehicle(v: Vehicle): Unit = {

    }

    private def changeVehicle(v: Vehicle): Unit = {

    }

    private def addCommodity(c: Commodity): Unit = {

    }

    private def deleteCommodity(c: Commodity): Unit = {

    }

    private def updateCommodity(c: Commodity): Unit = {

    }

    val vehicleEnum: Enumerator[(Events.Value,Vehicle)] = Vehicle.Dao.clusterSubscribe(cluster)
    val commodityEnum: Enumerator[(Events.Value,Commodity)] = Commodity.Dao.clusterSubscribe(cluster)

    vehicleEnum(Iteratee.foreach{
        case (Events.Created, v) => addVehicle(v)
        case (Events.Deleted, v) => deleteVehicle(v)
        case (Events.Updated, v) => updateVehicle(v)
    })

    commodityEnum(Iteratee.foreach{
        case (Events.Created, c) => addCommodity(c)
        case (Events.Deleted, c) => deleteCommodity(c)
        case (Events.Updated, c) => updateCommodity(c)
    })

    def vehicleSubscribe(v: Vehicle): Enumerator[Route] = {
        ???
    }
}
