package io.pathfinder.routing

import akka.actor.ActorRef
import io.pathfinder.models.{Cluster, Commodity, Vehicle}
import io.pathfinder.routing.Action.{DropOff, PickUp, Start}
import io.pathfinder.websockets.{ModelTypes, Events}
import io.pathfinder.websockets.WebSocketMessage.Routed
import play.Logger
import play.api.libs.iteratee.{Iteratee, Enumerator}
import scala.Predef
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.{Set,Builder}
import scala.collection.mutable
import Function.tupled

object Router {
    // This can be used to force the initialization of the static code in this class from Java.
    def init(): Unit = {}

    Logger.info("Now initializing Router singleton")

    // these should probably be thread safe eventually
    private val vehicles: mutable.Map[Vehicle,Set[ActorRef]] = mutable.Map[Vehicle,Set[ActorRef]]()
    private val commodities = mutable.Set[Commodity]()

    def addVehicle(v: Vehicle): Unit = {
        Logger.info("Adding Vehicle To routed collection: "+v)
        if (!vehicles.contains(v)) {
            vehicles.put(v, Set[ActorRef]())
        }
        recalculate()
    }

    def addVehicle(v: Vehicle, actor: ActorRef): Unit = {
        Logger.info("Adding Vehicle To routed collection: "+v)
        if (!vehicles.contains(v)) {
            vehicles.put(v, Set[ActorRef]())
        }
        vehicleSubscribe(v, actor)
        recalculate()
    }

    def addCommodity(c: Commodity): Unit = {
        Logger.info("Adding Commodity To routed collection: "+c)
        commodities += c
        recalculate()
    }

    // Dan wrote an infinite loop here.
    private def recalculate(): Unit = {
        if (vehicles.size <= 0) {
            Logger.info("Someone asked Router to recalculate but there are no vehicles in cluster.")
            return
        }
        Logger.info("Reticulating splines")
        val builders: Seq[mutable.Builder[Action, Seq[Action]]] = Seq.fill(vehicles.size) { Seq.newBuilder[Action]}
        var i = 0
        commodities.foreach {
            c => {
                Logger.info(String.format("Adding %s to a route", c))
                builders(i % vehicles.size) += new PickUp(c) += new DropOff(c)
                i += 1
            }
        }
        val routes: Seq[(Vehicle, Route)] = builders.zip(vehicles.keySet) map tupled { (builder, v) => (v, new Route(v.id, Seq(new Start(v)) ++ builder.result())) }
        routes.foreach {
            case (v,route) => {
                Logger.info(String.format("Notififying observers of new route for %s", v))
                vehicles.get(v).get.foreach{actor => actor ! Routed(ModelTypes.Vehicle, v.id, Route.writes.writes(route))}
            }
        }
        Logger.info("Finished recalculating routes")
    }

    val initThread = new Thread {
        // Dan also wrote an infinite loop here but we couldn't fix it so we put it in it's own thread.
        // Yes, it's still an infinite loop that happens on the first thread of the server.
        val vehicleEnum: Enumerator[(Events.Value, Vehicle)] = Vehicle.Dao.clusterSubscribe()
        val commodityEnum: Enumerator[(Events.Value, Commodity)] = Commodity.Dao.clusterSubscribe()

        vehicleEnum.run(Iteratee.foreach {
            case (event, v) => {
                Logger.info("Router received vehicle: " + v + " with event " + event)
                event match {
                    case (Events.Created) => Unit; //addVehicle(v)
                    case _ => Logger.error("Router cannot handle this event: "+event+" for model "+v)
                }
            }
        })

        commodityEnum.run(Iteratee.foreach {
            case (Events.Created, c) => Unit; //addCommodity(c)
            case (e, c) => Logger.error("Router cannot handle this event: "+e+" for model "+c)
        })
    }
    initThread.start

    /** If the vehicle has not yet been added, it will not be added. */
    def vehicleSubscribe(v: Vehicle, socket: ActorRef) {
        val observers: Option[mutable.Set[ActorRef]] = vehicles.get(v)
        if (observers.isDefined) {
            vehicles.put(v, vehicles.get(v).get + socket)
            Logger.info(String.format("%s now subscribed to %s", socket, v))
        } else {
            Logger.info(String.format("Failed to add subscriber for %s", v))
        }
    }
}
