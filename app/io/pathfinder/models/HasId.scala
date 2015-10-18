package io.pathfinder.models

import play.db.ebean.Model

trait HasId extends Model {
  def id: Long
}
