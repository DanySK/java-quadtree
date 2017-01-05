# Java QuadTree

Java spatial indexing tools. In particular, contains a quad tree implementation that works without knowing in advance dimensions and offset of the environment.

## Status

### Stable branch

[![Build Status](https://travis-ci.org/DanySK/java-quadtree.svg?branch=master)](https://travis-ci.org/DanySK/java-quadtree)

### Development branch

[![Build Status](https://travis-ci.org/DanySK/java-quadtree.svg?branch=develop)](https://travis-ci.org/DanySK/java-quadtree)

## Usage

``` java
// Build a quad tree
final SpatialIndex<Object> qt = new FlexibleQuadTree<>();
// Push data
qt.insert(new Object(), 1, 2);
// Returns a List<Object> containing the previously inserted object
List<Object> r = qt.query(new double[]{0, 0}, new double[]{2, 2});
// Object removal
qt.remove(r.get(0), 1, 2));
```

## Import in your project

I warmly suggest to use Gradle, Maven or a similar system to deal with dependencies within your project. In this case, you can use this product by importing the following Maven dependency:

```xml
<dependency>
    <groupId>org.danilopianini</groupId>
    <artifactId>java-quadtree</artifactId>
    <version>VERSION_YOU_WANT_TO_USE</version>
</dependency>
```

or the following Gradle dependency:

```Gradle
compile 'org.danilopianini:java-quadtree:VERSION_YOU_WANT_TO_USE'
```

Alternatively, you can grab the latest jar and throw it in your classpath. In this case, be sure to include the dependencies of this project in your classpath as well.


