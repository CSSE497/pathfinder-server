package io.pathfinder.models

import com.avaje.ebean.Model
import javax.persistence.{Id,Entity,GeneratedValue,GenerationType}
import play.data.validation.Constraints.Required
import play.api.libs.json.Json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/**
 * @author hansondg
 */

object Vehicle extends CrudCompanion[Long,Vehicle]{
    val finder: Model.Finder[Long,Vehicle] = new Model.Finder[Long,Vehicle](classOf[Vehicle])
}

@Entity
class Vehicle(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,

    @Required
    var latitude: Double,

    @Required
    var longitude: Double,

    @Required
    var capacity: Int
) extends Model