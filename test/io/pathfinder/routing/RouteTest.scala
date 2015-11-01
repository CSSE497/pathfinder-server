package io.pathfinder.routing

import io.pathfinder.models.{Vehicle, Commodity}
import io.pathfinder.routing.Action.{DropOff, PickUp, Start}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class RouteTest extends PlaySpec {

    val routeJson = Json.parse("""{
                "vehicle":{"id":9,"latitude":2,"longitude":3,"capacity":1,"status":"Online"},
                "actions":[
                    { "action":"start","latitude":12,"longitude":18},
                    { "action":"pickup",
                      "latitude":2,
                      "longitude":3,
                      "commodity":{"id":1,"startLatitude":2,"startLongitude":3,"endLatitude":4,"endLongitude":5,"param":6}},
                    { "action":"dropoff",
                      "latitude":4,
                      "longitude":5,
                      "commodity":{"id":1,"startLatitude":2,"startLongitude":3,"endLatitude":4,"endLongitude":5,"param":6}},
                    { "action":"pickup",
                      "latitude":8,
                      "longitude":9,
                      "commodity":{"id":7,"startLatitude":8,"startLongitude":9,"endLatitude":10,"endLongitude":11,"param":12}},
                    { "action":"dropoff",
                      "latitude":10,
                      "longitude":11,
                      "commodity":{"id":7,"startLatitude":8,"startLongitude":9,"endLatitude":10,"endLongitude":11,"param":12}},
                    { "action":"pickup",
                      "latitude":14,
                      "longitude":15,
                      "commodity":{"id":13,"startLatitude":14,"startLongitude":15,"endLatitude":16,"endLongitude":17,"param":18}},
                    { "action":"dropoff",
                      "latitude":16,
                      "longitude":17,
                      "commodity":{"id":13,"startLatitude":14,"startLongitude":15,"endLatitude":16,"endLongitude":17,"param":18}}
                ]
            }""")

    val coms = Seq(
        Commodity(1,2,3,4,5,6),
        Commodity(7,8,9,10,11,12),
        Commodity(13,14,15,16,17,18)
    )

    val actions = coms.foldLeft(Seq.newBuilder[Action]+=Start(12,18)) {
        (b,c) => b += new PickUp(c) += new DropOff(c)
    }.result()

    val vehicle = Vehicle(9, 2, 3, "Online", 1)

    val route = Route(vehicle,actions)

    "Route.writes" should {
        "write a route object into json" in {
            Route.writes.writes(route) mustBe routeJson
        }
    }
}
