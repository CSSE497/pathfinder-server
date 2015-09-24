package io.pathfinder.data

trait CrudDao[K,M] {

    def create(model: M): M

    def update(id: K, update: Update[M]): Option[M]

    def update(model: M): Option[M]

    def delete(id: K): Option[M]

    def read(id: K): Option[M]

    def readAll: Seq[M]
}
