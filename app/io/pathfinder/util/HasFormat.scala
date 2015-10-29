package io.pathfinder.util

import play.api.libs.json.Format

trait HasFormat extends Enumeration {
    implicit val format: Format[this.type#Value] = new EnumerationFormat[this.type](this)
}
