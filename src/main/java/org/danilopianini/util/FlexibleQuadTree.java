/*******************************************************************************
 * Copyright (C) 2009-2017, Danilo Pianini and contributors
 * listed in the project's build.gradle or pom.xml file.
 *
 * This file is distributed under the terms of the Apache License, version 2.0
 *******************************************************************************/
package org.danilopianini.util;

import com.google.common.hash.Hashing;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.nextDown;
import static java.lang.Math.nextUp;

/**
 * @param <E> the type of objects stored in the quadtree
 */
public final class FlexibleQuadTree<E> implements SpatialIndex<E> {

    /**
     * Default maximum number of entries per node.
     */
    public static final int DEFAULT_CAPACITY = 10;
    private static final long serialVersionUID = 0L;
    private final Map<Child, FlexibleQuadTree<E>> children;
    private final Deque<QuadTreeEntry<E>> elements;
    private final int maxElements;
    private Rectangle2D bounds;
    private FlexibleQuadTree<E> parent;

    /**
     * root is NOT consistent everywhere. It is only guaranteed to be consistent
     * in the entry point node and in the current root.
     */
    private FlexibleQuadTree<E> root;

    /**
     * Builds a {@link FlexibleQuadTree} with the default node capacity.
     */
    public FlexibleQuadTree() {
        this(DEFAULT_CAPACITY);
    }

    private FlexibleQuadTree(
        final double minx,
        final double maxx,
        final double miny,
        final double maxy,
        final int elemPerQuad,
        final FlexibleQuadTree<E> rootNode,
        final FlexibleQuadTree<E> parentNode
    ) {
        this(elemPerQuad, rootNode, parentNode);
        bounds = new Rectangle2D(minx, miny, maxx, maxy);
    }

    private FlexibleQuadTree(
        final int elemPerQuad,
        final FlexibleQuadTree<E> rootNode,
        final FlexibleQuadTree<E> parentNode
    ) {
        if (elemPerQuad < 2) {
            throw new IllegalArgumentException("At least two elements per quadtree are required for this index to work properly");
        }
        elements = new ArrayDeque<>(DEFAULT_CAPACITY);
        children = new EnumMap<>(Child.class);
        maxElements = elemPerQuad;
        parent = parentNode;
        root = rootNode == null ? this : rootNode;
    }

    /**
     * @param elemPerQuad maximum number of elements per quad
     */
    public FlexibleQuadTree(final int elemPerQuad) {
        this(elemPerQuad, null, null);
    }

    private double centerX() {
        return bounds.getCenterX();
    }

    private double centerY() {
        return bounds.getCenterY();
    }

    private boolean contains(final double x, final double y) {
        return bounds == null || bounds.contains(x, y);
    }

    private FlexibleQuadTree<E> create(
        final double minx,
        final double maxx,
        final double miny,
        final double maxy,
        final FlexibleQuadTree<E> father
    ) {
        return new FlexibleQuadTree<>(minx, maxx, miny, maxy, getMaxElementsNumber(), root, father);
    }

    private void createChildIfAbsent(final Child c) {
        children.putIfAbsent(c, create(minX(c), maxX(c), minY(c), maxY(c), this));
    }

    private void createParent(final double x, final double y) {
        /*
         * Determine where the parent should be
         */
        if (x < centerX()) {
            final double minx = 2 * minX() - maxX();
            if (y < centerY()) {
                /*
                 * This will be TR child of the new parent
                 */
                root = create(minx, maxX(), 2 * minY() - maxY(), maxY(), null);
                root.setChild(Child.TR, this);
            } else {
                /*
                 * This will be BR child of the new parent
                 */
                root = create(minx, maxX(), minY(), 2 * maxY() - minY(), null);
                root.setChild(Child.BR, this);
            }
        } else {
            final double maxx = 2 * maxX() - minX();
            if (y < centerY()) {
                /*
                 * This will be TL child of the new parent
                 */
                root = create(minX(), maxx, 2 * minY() - maxY(), maxY(), null);
                root.setChild(Child.TL, this);
            } else {
                /*
                 * This will be BL child of the new parent
                 */
                root = create(minX(), maxx, minY(), 2 * maxY() - minY(), null);
                root.setChild(Child.BL, this);
            }
        }
        /*
         * A bit cryptic, but the root of the new root is the root itself.
         * Otherwise, the root would point to the previous root.
         */
        root.root = root;
        root.subdivide();
    }

    @Override
    public int getDimensions() {
        return 2;
    }

    /**
     * @return the maximum number of elements per node
     */
    public int getMaxElementsNumber() {
        return maxElements;
    }

    private boolean hasSpace() {
        return elements.size() < maxElements;
    }

    @Override
    public void insert(final E e, final double... pos) {
        assert pos.length == 2;
        insert(e, pos[0], pos[1]);
    }

    /**
     * Same of {@link #insert(Object, double...)}, but with explicit parameters.
     *
     * @param e element
     * @param x X
     * @param y Y
     */
    public void insert(final E e, final double x, final double y) {
        if (bounds == null) {
            if (hasSpace()) {
                insertNode(e, x, y);
                return;
            }
            double minx = Double.POSITIVE_INFINITY;
            double miny = Double.POSITIVE_INFINITY;
            double maxx = Double.NEGATIVE_INFINITY;
            double maxy = Double.NEGATIVE_INFINITY;
            for (final QuadTreeEntry<E> element : elements) {
                minx = min(minx, element.x);
                miny = min(miny, element.y);
                maxx = max(maxx, element.x);
                maxy = max(maxy, element.y);
            }
            assert Double.isFinite(minx);
            assert Double.isFinite(maxx);
            assert Double.isFinite(miny);
            assert Double.isFinite(maxy);
            bounds = new Rectangle2D(floor(nextDown(minx)), floor(nextDown(miny)), ceil(nextUp(maxx)), ceil(nextUp(maxy)));
        }
        /*
         * I must insert starting from the root. If the root does not contain
         * the coordinates, then the tree should be expanded upwards
         */
        while (!root.contains(x, y)) {
            root.createParent(x, y);
            root = root.root;
        }
        root.insertHere(e, x, y);
    }

    private void insertHere(final E e, final double x, final double y) {
        if (hasSpace()) {
            insertNode(e, x, y);
        } else {
            if (children.isEmpty()) {
                subdivide();
            }
            selectChild(x, y).insertHere(e, x, y);
        }
    }

    private void insertNode(@Nonnull final E e, final double x, final double y) {
        assert elements.size() < maxElements : "Bug in " + getClass() + ". Forced insertion over the container size.";
        elements.push(new QuadTreeEntry<>(e, x, y));
    }

    private double maxX() {
        return bounds.getMaxX();
    }

    private double maxX(final Child c) {
        switch (c) {
            case TR:
            case BR:
                return maxX();
            case BL:
            case TL:
                return centerX();
            default:
                throw new IllegalStateException();
        }
    }

    private double maxY() {
        return bounds.getMaxY();
    }

    private double maxY(final Child c) {
        switch (c) {
            case BL:
            case BR:
                return centerY();
            case TR:
            case TL:
                return maxY();
            default:
                throw new IllegalStateException();
        }
    }

    private double minX() {
        return bounds.getMinX();
    }

    private double minX(final Child c) {
        switch (c) {
            case TR:
            case BR:
                return centerX();
            case BL:
            case TL:
                return minX();
            default:
                throw new IllegalStateException();
        }
    }

    private double minY() {
        return bounds.getMinY();
    }

    private double minY(final Child c) {
        switch (c) {
            case BL:
            case BR:
                return minY();
            case TR:
            case TL:
                return centerY();
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Same of {@link #move(Object, double[], double[])}, but with explicit
     * parameters.
     *
     * @param e  the element
     * @param sx the start x
     * @param sy the start y
     * @param fx the final x
     * @param fy the final y
     *
     * @return true if the element is found and no error occurred
     */
    public boolean move(final E e, final double sx, final double sy, final double fx, final double fy) {
        return moveFromNode(root, e, sx, sy, fx, fy);
    }

    @Override
    public boolean move(final E e, final double[] start, final double[] end) {
        assert start.length == 2;
        assert end.length == 2;
        return move(e, start[0], start[1], end[0], end[1]);
    }

    private boolean moveFromNode(
            final FlexibleQuadTree<E> root,
            final E e,
            final double sx, final double sy,
            final double fx, final double fy) {
        final QuadTreeEntry<E> toRemove = new QuadTreeEntry<>(e, sx, sy);
        for (FlexibleQuadTree<E> cur = root; cur.contains(sx, sy); cur = cur.selectChild(sx, sy)) {
            if (cur.elements.remove(toRemove)) {
                /*
                 * Node found.
                 */
                if (cur.contains(fx, fy)) {
                    /*
                     * Moved within the same quadrant.
                     */
                    cur.insertNode(e, fx, fy);
                } else if (
                    cur.parent == null
                    || !cur.parent.contains(fx, fy)
                    || !cur.swapMostStatic(e, fx, fy)
                ) {
                    /*
                     * In case:
                     *  - we are the root
                     *  - we moved outside the parent's area
                     *  - the swapping operation failed
                     */
                    insert(e, fx, fy);
                }
                return true;
            }
            if (cur.children.isEmpty()) {
                return false;
            }
        }
        return false;
    }

    /**
     * Same of querying with arrays, but with explicit parameters.
     *
     * @param x1 Rectangle X coordinate of the first point
     * @param y1 Rectangle Y coordinate of the first point
     * @param x2 Rectangle X coordinate of the second point
     * @param y2 Rectangle Y coordinate of the second point
     *
     * @return {@link List} of Objects in range.
     */
    public List<E> query(final double x1, final double y1, final double x2, final double y2) {
        final List<E> result = new ArrayList<>();
        root.query(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), result);
        return result;
    }

    private void query(// NOPMD: False positive
        final double sx,
        final double sy,
        final double fx,
        final double fy,
        final List<E> results
    ) {
        assert !(bounds == null && !children.isEmpty());
        if (bounds == null || bounds.intersects(sx, sy, fx, fy)) {
            for (final QuadTreeEntry<E> entry : elements) {
                if (entry.isIn(sx, sy, fx, fy)) {
                    results.add(entry.element);
                }
            }
            // If there are no children, this will skip them.
            for (final FlexibleQuadTree<E> childOpt : children.values()) {
                childOpt.query(sx, sy, fx, fy, results);
            }
        }
    }

    @Override
    public List<E> query(final double[]... space) {
        if (space.length != 2 || space[0].length != 2 || space[1].length != 2) {
            throw new IllegalArgumentException();
        }
        return query(space[0][0], space[0][1], space[1][0], space[1][1]);
    }

    @Override
    public boolean remove(final E e, final double... pos) {
        assert pos.length == 2;
        return remove(e, pos[0], pos[1]);
    }

    /**
     * Same of {@link #remove(Object, double...)} with explicit parameters.
     *
     * @param e Element to remove
     * @param x X position of the element
     * @param y Y position of the element
     *
     * @return true if the element has been found and removed
     */
    public boolean remove(final E e, final double x, final double y) {
        return root.removeHere(e, x, y);
    }

    private boolean removeHere(final E e, final double x, final double y) {
        if (contains(x, y)) {
            return elements.remove(new QuadTreeEntry<>(e, x, y)) || removeInChildren(e, x, y);
        }
        return false;
    }

    private boolean removeInChildren(final E e, final double x, final double y) {
        return children.values().stream().anyMatch(c -> c.removeHere(e, x, y));
    }

    private FlexibleQuadTree<E> selectChild(final double x, final double y) {
        assert !children.isEmpty();
        if (x < centerX()) {
            return y < centerY() ? children.get(Child.BL) : children.get(Child.TL);
        } else {
            return y < centerY() ? children.get(Child.BR) : children.get(Child.TR);
        }
    }

    private void setChild(final Child c, final FlexibleQuadTree<E> child) {
        if (children.put(c, child) != null) {
            throw new IllegalStateException();
        }
        child.parent = this;
    }

    private void subdivide() {
        for (final Child c : Child.values()) {
            createChildIfAbsent(c);
        }
    }

    private boolean swapMostStatic(final E e, final double fx, final double fy) {
        assert parent != null : "Tried to swap on a null parent.";
        final Iterator<QuadTreeEntry<E>> iterator = parent.elements.descendingIterator();
        while (iterator.hasNext()) {
            final QuadTreeEntry<E> target = iterator.next();
            if (contains(target.x, target.y)) {
                /*
                 * There is a swappable node
                 */
                iterator.remove();
                elements.push(target);
                parent.insertNode(e, fx, fy);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return (bounds == null ? "Unbounded" : bounds.toString()) + ":" + elements.toString();
    }

    private enum Child {
        TR, BR, BL, TL
    }

    private static class QuadTreeEntry<E> implements Serializable {
        private static final long serialVersionUID = 9021533648086596986L;
        private final E element;
        private final double x, y;

        QuadTreeEntry(final E el, final double xp, final double yp) {
            element = el;
            x = xp;
            y = yp;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof QuadTreeEntry<?>) {
                final QuadTreeEntry<?> e = (QuadTreeEntry<?>) obj;
                // NOPMD: Pointer comparison intended
                return samePosition(e) && Objects.equals(element, e.element);
            }
            return false;
        }

        @Override
        @SuppressWarnings("UnstableApiUsage")
        public int hashCode() {
            return Hashing.murmur3_32_fixed().newHasher()
                .putDouble(x)
                .putDouble(y)
                .putInt(element.hashCode())
                .hash()
                .asInt();
        }

        public boolean isIn(final double sx, final double sy, final double fx, final double fy) {
            return x >= sx && x < fx && y >= sy && y < fy;
        }

        public boolean samePosition(final QuadTreeEntry<?> target) {
            return x == target.x && y == target.y;
        }

        @Override
        public String toString() {
            return element.toString() + "@[" + x + ", " + y + "]";
        }
    }

    private static class Rectangle2D implements Serializable {
        private static final long serialVersionUID = -7890062202005580979L;
        private final double minx, miny, maxx, maxy;

        Rectangle2D(final double sx, final double sy, final double fx, final double fy) {
            minx = min(sx, fx);
            miny = min(sy, fy);
            maxx = max(sx, fx);
            maxy = max(sy, fy);
        }

        public boolean contains(final double x, final double y) {
            return x >= minx && y >= miny && x < maxx && y < maxy;
        }

        public double getCenterX() {
            return minx + (maxx - minx) / 2;
        }

        public double getCenterY() {
            return miny + (maxy - miny) / 2;
        }

        public double getMaxX() {
            return maxx;
        }

        public double getMaxY() {
            return maxy;
        }

        public double getMinX() {
            return minx;
        }

        public double getMinY() {
            return miny;
        }

        public boolean intersects(final double sx, final double sy, final double fx, final double fy) {
            return fx >= minx && fy >= miny && sx < maxx && sy < maxy;
        }

        @Override
        public String toString() {
            return "[" + minx + "," + miny + " - " + maxx + "," + maxy + "]";
        }
    }
}
