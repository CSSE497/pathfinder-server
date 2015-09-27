package io.pathfinder.data

/**
 * An update is a functor that updates an model(without saving it), it
 * should return true if the update is a full update, where all fields required
 * for a create are set, otherwise, it returns false.
 */
trait Update[M] {
    def apply(model: M): Boolean
}
