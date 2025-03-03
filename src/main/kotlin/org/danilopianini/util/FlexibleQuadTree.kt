/*
 * Copyright (C) 2009-2017, Danilo Pianini and contributors
 * listed in the project's build.gradle or pom.xml file.
 * This file is distributed under the terms of the Apache License, version 2.0
 */
package org.danilopianini.util

import java.io.Serializable
import java.util.ArrayDeque
import java.util.Deque
import java.util.EnumMap
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.nextDown
import kotlin.math.nextUp

/**
 * @param <E> the type of objects stored in the quadtree
 */
class FlexibleQuadTree<E> private constructor(
    elementsPerQuad: Int,
    rootNode: FlexibleQuadTree<E>?,
    parentNode: FlexibleQuadTree<E>?,
) : SpatialIndex<E> {
    private val children: MutableMap<Child, FlexibleQuadTree<E>>
    private val elements: Deque<QuadTreeEntry<E>>

    /**
     * @return the maximum number of elements per node
     */
    val maxElementsNumber: Int
    private var bounds: Rectangle2D? = null
    private var parent: FlexibleQuadTree<E>?

    /**
     * root is NOT consistent everywhere. It is only guaranteed to be consistent
     * in the entry point node and in the current root.
     */
    private var root: FlexibleQuadTree<E>

    private constructor(
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double,
        elemPerQuad: Int,
        rootNode: FlexibleQuadTree<E>?,
        parentNode: FlexibleQuadTree<E>?,
    ) : this(elemPerQuad, rootNode, parentNode) {
        bounds = Rectangle2D(minX, minY, maxX, maxY)
    }

    init {
        require(elementsPerQuad >= 2) {
            "At least two elements per quadtree are required for this index to work properly"
        }
        elements = ArrayDeque<QuadTreeEntry<E>>(DEFAULT_CAPACITY)
        children = EnumMap<Child, FlexibleQuadTree<E>>(Child::class.java)
        this.maxElementsNumber = elementsPerQuad
        parent = parentNode
        root = rootNode ?: this
    }

    /**
     * Builds a [FlexibleQuadTree] with the default node capacity.
     * @param elemPerQuad maximum number of elements per quad
     */
    @JvmOverloads
    constructor(elemPerQuad: Int = DEFAULT_CAPACITY) : this(elemPerQuad, null, null)

    private val centerX: Double get() = checkNotNull(bounds).centerX

    private val centerY: Double get() = checkNotNull(bounds).centerY

    private fun contains(
        x: Double,
        y: Double,
    ): Boolean = bounds == null || checkNotNull(bounds).contains(x, y)

    private fun create(
        minX: Double,
        maxX: Double,
        minY: Double,
        maxY: Double,
        father: FlexibleQuadTree<E>?,
    ): FlexibleQuadTree<E> = FlexibleQuadTree<E>(minX, maxX, minY, maxY, this.maxElementsNumber, root, father)

    private fun createChildIfAbsent(c: Child) {
        children.putIfAbsent(c, create(minX(c), maxX(c), minY(c), maxY(c), this))
    }

    private fun createParent(
        x: Double,
        y: Double,
    ) {
        /*
         * Determine where the parent should be
         */
        if (x < centerX) {
            val minx = 2 * minX - maxX
            if (y < centerY) {
                /*
                 * This will be the TR child of the new parent
                 */
                root = create(minx, maxX, 2 * minY - maxY, maxY, null)
                root.setChild(Child.TR, this)
            } else {
                /*
                 * This will be the BR child of the new parent
                 */
                root = create(minx, maxX, minY, 2 * maxY - minY, null)
                root.setChild(Child.BR, this)
            }
        } else {
            val maxX = 2 * maxX - minX
            if (y < centerY) {
                /*
                 * This will be the TL child of the new parent
                 */
                root = create(minX, maxX, 2 * minY - maxY, maxY, null)
                root.setChild(Child.TL, this)
            } else {
                /*
                 * This will be the BL child of the new parent
                 */
                root = create(minX, maxX, minY, 2 * maxY - minY, null)
                root.setChild(Child.BL, this)
            }
        }
        /*
         * A bit cryptic, but the root of the new root is the root itself.
         * Otherwise, the root would point to the previous root.
         */
        root.root = root
        root.subdivide()
    }

    override val dimensions: Int = 2

    private fun hasSpace(): Boolean = elements.size < this.maxElementsNumber

    override fun insert(
        element: E,
        vararg pos: Double,
    ) {
        assert(pos.size == 2)
        insert(element, pos[0], pos[1])
    }

    /**
     * Same of [.insert], but with explicit parameters.
     *
     * @param element element
     * @param x X
     * @param y Y
     */
    fun insert(
        element: E,
        x: Double,
        y: Double,
    ) {
        if (bounds == null) {
            if (hasSpace()) {
                insertNode(element, x, y)
                return
            }
            var minX = Double.Companion.POSITIVE_INFINITY
            var minY = Double.Companion.POSITIVE_INFINITY
            var maxX = Double.Companion.NEGATIVE_INFINITY
            var maxY = Double.Companion.NEGATIVE_INFINITY
            for (currentElement in elements) {
                minX = min(minX, currentElement.x)
                minY = min(minY, currentElement.y)
                maxX = max(maxX, currentElement.x)
                maxY = max(maxY, currentElement.y)
            }
            assert(java.lang.Double.isFinite(minX))
            assert(java.lang.Double.isFinite(maxX))
            assert(java.lang.Double.isFinite(minY))
            assert(java.lang.Double.isFinite(maxY))
            bounds =
                Rectangle2D(floor(minX.nextDown()), floor(minY.nextDown()), ceil(maxX.nextUp()), ceil(maxY.nextUp()))
        }
        /*
         * I must insert starting from the root. If the root does not contain
         * the coordinates, then the tree should be expanded upwards
         */
        while (!root.contains(x, y)) {
            root.createParent(x, y)
            root = root.root
        }
        root.insertHere(element, x, y)
    }

    private fun insertHere(
        e: E,
        x: Double,
        y: Double,
    ) {
        if (hasSpace()) {
            insertNode(e, x, y)
        } else {
            if (children.isEmpty()) {
                subdivide()
            }
            selectChild(x, y).insertHere(e, x, y)
        }
    }

    private fun insertNode(
        e: E,
        x: Double,
        y: Double,
    ) {
        assert(elements.size < this.maxElementsNumber) {
            "Bug in $javaClass. Forced insertion over the container size."
        }
        elements.push(QuadTreeEntry<E>(e, x, y))
    }

    private val maxX: Double get() = checkNotNull(bounds).maxX

    private fun maxX(c: Child): Double =
        when (c) {
            Child.TR, Child.BR -> maxX
            Child.BL, Child.TL -> centerX
        }

    private val maxY: Double get() = checkNotNull(bounds).maxY

    private fun maxY(c: Child): Double =
        when (c) {
            Child.BL, Child.BR -> centerY
            Child.TR, Child.TL -> maxY
        }

    private val minX: Double get() = checkNotNull(bounds).minX

    private fun minX(c: Child): Double =
        when (c) {
            Child.TR, Child.BR -> centerX
            Child.BL, Child.TL -> minX
        }

    private val minY: Double get() = checkNotNull(bounds).minY

    private fun minY(c: Child): Double =
        when (c) {
            Child.BL, Child.BR -> minY
            Child.TR, Child.TL -> centerY
        }

    /**
     * Same of [.move], but with explicit
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
    fun move(
        e: E,
        sx: Double,
        sy: Double,
        fx: Double,
        fy: Double,
    ): Boolean {
        val toRemove = QuadTreeEntry<E>(e, sx, sy)
        var cur = root
        var moved = false
        while (cur.contains(sx, sy) && !moved) {
            when {
                cur.elements.remove(toRemove) -> {
                    /*
                     * Node found.
                     */
                    moved = true
                    val currentParent = cur.parent
                    if (cur.contains(fx, fy)) {
                        /*
                         * Moved within the same quadrant.
                         */
                        cur.insertNode(e, fx, fy)
                    } else if (
                        // We are root, or
                        currentParent == null ||
                        // we moved outside the parent's area, or
                        !currentParent.contains(fx, fy) ||
                        // the swapping operation failed
                        !cur.swapMostStatic(e, fx, fy)
                    ) {
                        insert(e, fx, fy)
                    }
                }
                cur.children.isEmpty() -> break
                else -> cur = cur.selectChild(sx, sy)
            }
        }
        return moved
    }

    override fun move(
        e: E,
        start: DoubleArray,
        end: DoubleArray,
    ): Boolean {
        assert(start.size == 2)
        assert(end.size == 2)
        return move(e, start[0], start[1], end[0], end[1])
    }

    /**
     * Same of querying with arrays, but with explicit parameters.
     *
     * @param x1 Rectangle X coordinate of the first point
     * @param y1 Rectangle Y coordinate of the first point
     * @param x2 Rectangle X coordinate of the second point
     * @param y2 Rectangle Y coordinate of the second point
     *
     * @return [List] of Objects in range.
     */
    fun query(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): MutableList<E> {
        val result: MutableList<E> = ArrayList<E>()
        root.query(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), result)
        return result
    }

    private fun query(
        sx: Double,
        sy: Double,
        fx: Double,
        fy: Double,
        results: MutableList<E>,
    ) {
        assert(!(bounds == null && !children.isEmpty()))
        if (bounds == null || checkNotNull(bounds).intersects(sx, sy, fx, fy)) {
            for (entry in elements) {
                if (entry.isIn(sx, sy, fx, fy)) {
                    results.add(entry.element)
                }
            }
            // If there are no children, this will skip them.
            for (childOpt in children.values) {
                childOpt.query(sx, sy, fx, fy, results)
            }
        }
    }

    override fun query(vararg space: DoubleArray): MutableList<E> {
        require(space.size == 2 && space[0].size == 2 && space[1].size == 2) {
            "Invalid space definition"
        }
        return query(space[0][0], space[0][1], space[1][0], space[1][1])
    }

    override fun remove(
        e: E,
        vararg pos: Double,
    ): Boolean {
        assert(pos.size == 2)
        return remove(e, pos[0], pos[1])
    }

    /**
     * Same of [.remove] with explicit parameters.
     *
     * @param e Element to remove
     * @param x X position of the element
     * @param y Y position of the element
     *
     * @return true if the element has been found and removed
     */
    fun remove(
        e: E,
        x: Double,
        y: Double,
    ): Boolean = root.removeHere(e, x, y)

    private fun removeHere(
        e: E,
        x: Double,
        y: Double,
    ): Boolean {
        if (contains(x, y)) {
            return elements.remove(QuadTreeEntry<E>(e, x, y)) || removeInChildren(e, x, y)
        }
        return false
    }

    private fun removeInChildren(
        e: E,
        x: Double,
        y: Double,
    ): Boolean = children.values.any { it.removeHere(e, x, y) }

    private fun selectChild(
        x: Double,
        y: Double,
    ): FlexibleQuadTree<E> {
        require(children.isNotEmpty())
        val result =
            when {
                x < centerX && y < centerY -> children[Child.BL]
                x < centerX && y >= centerY -> children[Child.TL]
                x >= centerX && y < centerY -> children[Child.BR]
                else -> children[Child.TR]
            }
        return checkNotNull(result) {
            "Child node at ($x, $y) is null. " +
                "This indicates an inconsistency in the quadtree structure, as all children should be initialized."
        }
    }

    private fun setChild(
        c: Child,
        child: FlexibleQuadTree<E>,
    ) {
        check(children.put(c, child) == null)
        child.parent = this
    }

    private fun subdivide() {
        for (c in Child.entries) {
            createChildIfAbsent(c)
        }
    }

    private fun swapMostStatic(
        e: E,
        fx: Double,
        fy: Double,
    ): Boolean {
        val myParent = checkNotNull(parent) { "Tried to swap on a null parent." }
        val iterator = myParent.elements.descendingIterator()
        while (iterator.hasNext()) {
            val target = iterator.next()
            if (contains(target.x, target.y)) {
                /*
                 * There is a swappable node
                 */
                iterator.remove()
                elements.push(target)
                myParent.insertNode(e, fx, fy)
                return true
            }
        }
        return false
    }

    override fun toString(): String = (bounds?.toString() ?: "Unbounded") + ":" + elements.toString()

    private enum class Child {
        TR,
        BR,
        BL,
        TL,
    }

    private data class QuadTreeEntry<E>(
        val element: E,
        val x: Double,
        val y: Double,
    ) : Serializable {
        fun isIn(
            sx: Double,
            sy: Double,
            fx: Double,
            fy: Double,
        ): Boolean = x >= sx && x < fx && y >= sy && y < fy

        override fun toString(): String = "$element@[$x, $y]"

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private class Rectangle2D(
        sx: Double,
        sy: Double,
        fx: Double,
        fy: Double,
    ) : Serializable {
        val minX: Double = min(sx, fx)
        val minY: Double = min(sy, fy)
        val maxX: Double = max(sx, fx)
        val maxY: Double = max(sy, fy)

        fun contains(
            x: Double,
            y: Double,
        ): Boolean = x >= this.minX && y >= this.minY && x < this.maxX && y < this.maxY

        val centerX: Double
            get() = this.minX + (this.maxX - this.minX) / 2

        val centerY: Double
            get() = this.minY + (this.maxY - this.minY) / 2

        fun intersects(
            sx: Double,
            sy: Double,
            fx: Double,
            fy: Double,
        ): Boolean = fx >= this.minX && fy >= this.minY && sx < this.maxX && sy < this.maxY

        override fun toString(): String = "[" + this.minX + "," + this.minY + " - " + this.maxX + "," + this.maxY + "]"

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    /**
     * Constants.
     */
    companion object {
        /**
         * Default maximum number of entries per node.
         */
        const val DEFAULT_CAPACITY: Int = 10
    }
}
