package io.pathfinder.data

abstract class CrudDao[K,M] {

    /**
     *  returns the default model should only constructs it, not save it
     */
    def construct: M

    /**
     * Creates, saves, and returns a default model
     */
    final def create: M = create(construct)

    /**
     * Adds the specified model to the database
     */
    def create(model: M): M

    /**
     * Creates a model according to the specified resource, if the update returns
     * false, no model is created in the database and None is returned
     */
    def create(create: Resource[M]): Option[M]

    /**
     * applies the resource as an update to the model with the given id,
     * returns the model if it exists, otherwise returns None
     */
    def update(id: K, update: Resource[M]): Option[M]

    /**
     * updates the specified model
     */
    def update(model: M): Option[M]

    /**
     * deletes the model with the specified id
     */
    def delete(id: K): Option[M]

    /**
     * returns the model with the specified id, if the id does not exist,
     * None is returned instead
     */
    def read(id: K): Option[M]

    /**
     * returns all of the models as a Seq
     */
    def readAll: Seq[M]
}
