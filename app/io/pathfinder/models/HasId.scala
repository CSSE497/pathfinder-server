package io.pathfinder.models

import com.avaje.ebean.Model

trait HasId extends Model {
    def id: Long
}
