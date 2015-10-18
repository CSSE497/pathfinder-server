package io.pathfinder.models

import play.db.ebean.Model

trait HasCluster extends Model {
  def cluster: Cluster
}
