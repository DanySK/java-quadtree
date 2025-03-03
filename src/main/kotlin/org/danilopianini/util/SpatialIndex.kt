package org.danilopianini.util

import java.io.Serializable

/**
 * @param E content of the index
 */
interface SpatialIndex<E> : Serializable {
    /**
     * Inserts an element in the [SpatialIndex].
     *
     * @param element The element to add
     * @param position The coordinates where the element should be added
     */
    fun insert(
        element: E,
        vararg position: Double,
    )

    /**
     * Deletes an element from the [SpatialIndex].
     *
     * @param element The element to remove
     * @param position The coordinates where the element should be removed from
     * @return true if the element is found and removed
     */
    fun remove(
        element: E,
        vararg position: Double,
    ): Boolean

    /**
     * If an element is moved, updates the [SpatialIndex] accordingly.
     *
     * @param element The element to move
     * @param start The coordinates where the element is currently located
     * @param end The coordinates where the element should be moved
     * @return true if the element is found and moved
     */
    fun move(
        element: E,
        start: DoubleArray,
        end: DoubleArray,
    ): Boolean

    /**
     * Queries the [SpatialIndex], searching for elements in a parallelotope.
     * "Parallelotope" is a fancy word for "N-dimensional rectangle".
     * A 2-parallelotope is a rectangle, a 3-parallelotope is a parallelepiped, and so on.
     *
     * @param parallelotope The space where to search for elements
     * @return The list of elements in this area of the [SpatialIndex].
     */
    fun query(vararg parallelotope: DoubleArray): List<E>

    /**
     * @return The number of dimensions of space for this [SpatialIndex].
     */
    val dimensions: Int
}
