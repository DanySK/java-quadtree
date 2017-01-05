package org.danilopianini.util;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @param <E> content of the index
 */
public interface SpatialIndex<E> extends Serializable {

    /**
     * Inserts an element in the {@link SpatialIndex}.
     * 
     * @param element
     *            The element to add
     * @param position
     *            the coordinates where the element should be added
     */
    void insert(E element, double... position);

    /**
     * Deletes an element from the {@link SpatialIndex}.
     * 
     * @param element
     *            The element to add
     * @param position
     *            the coordinates where the element should be added
     * @return true if the element is found and removed
     */
    boolean remove(E element, double... position);

    /**
     * If an element is moved, updates the {@link SpatialIndex} accordingly.
     * 
     * @param element
     *            The element to add
     * @param start
     *            the coordinates where the element is currently located
     * @param end
     *            the coordinates where the element is should be moved
     * @return true if the element is found and moved
     */
    boolean move(E element, double[] start, double[] end);

    /**
     * Queries the {@link SpatialIndex}, searching for elements in a
     * parallelotope. "Parallelotope" is a fancy word for
     * "N-dimensional rectangle". A 2-parallelotope is in fact a rectangle, a
     * 3-parallelotope is a parallelepiped, and so on.
     * 
     * @param parallelotope
     *            the space where to search for elements
     * @return the list of elements in this area of the {@link SpatialIndex}.
     */
    List<E> query(double[]... parallelotope);

    /**
     * @return the number of dimension of space for this {@link SpatialIndex}.
     */
    int getDimensions();

}
