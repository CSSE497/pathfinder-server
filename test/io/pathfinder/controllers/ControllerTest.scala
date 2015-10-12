package io.pathfinder.controllers

import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

abstract class ControllerTest extends PlaySpec with MustMatchers with FutureAwaits with DefaultAwaitTimeout
