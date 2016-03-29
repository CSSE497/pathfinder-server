package io.pathfinder.authentication

import io.pathfinder.util.HasFormat

object AuthenticationStatus extends Enumeration with HasFormat {
    type AuthenticationStatus = Value
    val Authenticated, UnauthorizedUser = Value
}
