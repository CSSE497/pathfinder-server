package io.pathfinder.data

/**
 * A resource is what is received and sent to the API clients
 */
trait Resource[M] {

    protected type R <: Resource[M]

    /**
     * updates the specified model with the resource's fields
     * @param model
     * @return
     */
    def update(model: M): Option[M]

    /**
     * creates a new model instance from this resource
     * @return
     */
    def create: Option[M]

    def withAppId(appId: String): Option[R]

    def withoutAppId: R
}
