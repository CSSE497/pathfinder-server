package io.pathfinder.routing

import io.pathfinder.models.{Cluster, Application, CommodityStatus, VehicleStatus, Vehicle, Commodity}
import io.pathfinder.routing.Action.{DropOff, PickUp, Start}
import org.scalatestplus.play.PlaySpec
import org.specs2.execute
import org.specs2.execute.AsResult
import play.api.libs.json.{JsNumber, JsObject, Json}
import play.api.test.{FakeApplication, Helpers, WithApplication}

class RouteTest extends PlaySpec {

    val APP_ID = "cluster"

    class FakeApp extends WithApplication(FakeApplication(additionalConfiguration = Helpers.inMemoryDatabase())) {

        override def around[T : AsResult](t: => T) = {
            Helpers.running(app){
                val pathfinderApp = new Application()
                pathfinderApp.id = APP_ID
                pathfinderApp.name = "my cluster"
                pathfinderApp.insert()
                val cluster = new Cluster()
                cluster.id = APP_ID
                cluster.insert()
                AsResult.effectively(t)
            }
        }
    }

    val routeJson = Json.parse("""{
        "vehicle":{"id":9,"latitude":2,"longitude":3,"status":"Online","metadata":{"capacity":1},"commodities":[],"clusterId":"cluster"},
        "actions":[
            { "action":"Start","latitude":12,"longitude":18},
            { "action":"PickUp",
              "latitude":2,
              "longitude":3,
              "commodity":{"id":1,"startLatitude":2,"startLongitude":3,"endLatitude":4,"endLongitude":5,"status":"Waiting","metadata":{"param":6},"clusterId":"cluster"}},
            { "action":"DropOff",
              "latitude":4,
              "longitude":5,
              "commodity":{"id":1,"startLatitude":2,"startLongitude":3,"endLatitude":4,"endLongitude":5,"status":"Waiting","metadata":{"param":6},"clusterId":"cluster"}},
            { "action":"PickUp",
              "latitude":8,
              "longitude":9,
              "commodity":{"id":7,"startLatitude":8,"startLongitude":9,"endLatitude":10,"endLongitude":11,"status":"Waiting","metadata":{"param":12},"clusterId":"cluster"}},
            { "action":"DropOff",
              "latitude":10,
              "longitude":11,
              "commodity":{"id":7,"startLatitude":8,"startLongitude":9,"endLatitude":10,"endLongitude":11,"status":"Waiting","metadata":{"param":12},"clusterId":"cluster"}},
            { "action":"PickUp",
              "latitude":14,
              "longitude":15,
              "commodity":{"id":13,"startLatitude":14,"startLongitude":15,"endLatitude":16,"endLongitude":17,"status":"Waiting","metadata":{"param":18},"clusterId":"cluster"}},
            { "action":"DropOff",
              "latitude":16,
              "longitude":17,
              "commodity":{"id":13,"startLatitude":14,"startLongitude":15,"endLatitude":16,"endLongitude":17,"status":"Waiting","metadata":{"param":18},"clusterId":"cluster"}}
        ]
    }""")

    "Route.writes" should {
        "write a route object into json" in new FakeApp {
            val coms = Seq(
                Commodity(1,2,3,4,5,CommodityStatus.Waiting,JsObject(Seq("param" -> JsNumber(6))), None, "cluster"),
                Commodity(7,8,9,10,11,CommodityStatus.Waiting,JsObject(Seq("param" -> JsNumber(12))), None, "cluster"),
                Commodity(13,14,15,16,17,CommodityStatus.Waiting,JsObject(Seq("param" -> JsNumber(18))), None, "cluster")
            )
            val actions = coms.foldLeft(Seq.newBuilder[Action]+=Start(12,18)) {
                (b,c) => b += new PickUp(c) += new DropOff(c)
            }.result()
            val vehicle = Vehicle(9, 2, 3, VehicleStatus.Online,JsObject(Seq("capacity" -> JsNumber(1))), None, "cluster")
            val route = Route(vehicle,actions)
            Route.writes.writes(route) mustBe routeJson
        }
    }
}
