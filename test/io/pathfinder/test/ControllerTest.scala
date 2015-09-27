package io.pathfinder.test

import org.scalatestplus.play.PlaySpec
import org.scalatest.MustMatchers
import play.api.test.{FutureAwaits,DefaultAwaitTimeout}

abstract class ControllerTest extends PlaySpec with MustMatchers with FutureAwaits with DefaultAwaitTimeout
