package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence.{Id,Entity,GeneratedValue,GenerationType}
import play.data.validation.Constraints.Required
import play.api.libs.json.Json
import play.api.libs.json.{Reads,Writes}
import play.api.libs.functional.syntax._

/**
 * @author hansondg
 */

//object Vehicle extends CrudCompanion[Long,Vehicle]{
//    override val finder: Model.Finder[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])
//    override object writes extends Writes[Vehicle] {
//        override def writes(v: Vehicle) = Json.obj(
//            "id" -> v.id.longValue(),
//            "position" -> Json.obj(
//                "lat" -> v.latitude.doubleValue(),
//                "lng" -> v.longitude.doubleValue()
//            ),
//            "capacity" -> v.capacity.intValue()
//        )
//    }
//}
//
//@Entity
//class Vehicle(
//    @Id
//    @GeneratedValue(strategy = GenerationType.AUTO)
//    var id: java.lang.Long,
//
//    @Required
//    var latitude: java.lang.Double,
//
//    @Required
//    var longitude: java.lang.Double,
//
//    @Required
//    var capacity: java.lang.Integer
//) extends Model