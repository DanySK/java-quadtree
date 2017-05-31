package org.danilopianini.util.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.danilopianini.util.FlexibleQuadTree;
import org.danilopianini.util.SpatialIndex;
import org.junit.Test;

/**
 */
public class TestFlexibleQuadTree {

    private static final int INSERTIONS = 100000;
    private static final int SUB_INS = INSERTIONS / 4;
    private static final Object TOKEN = "";

    /**
     * 
     */
    @Test
    public void testRandom() {
        final Random rnd = new Random(0);
        final List<double[]> startPositions = makeRandomTest(rnd);
        final FlexibleQuadTree<Object> qt = new FlexibleQuadTree<>();
        /*
         * Test that everything got inserted
         */
        startPositions.stream().forEach(o -> qt.insert(TOKEN, o[0], o[1]));
        assertEquals(INSERTIONS, qt.query(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).size());
        /*
         * Move everything and test that it got moved
         */
        final List<double[]> moveTo = makeRandomTest(rnd);
        testMove(qt, startPositions, moveTo);
        /*
         * Move again
         */
        final List<double[]> moveToAgain = makeRandomTest(rnd);
        testMove(qt, moveTo, moveToAgain);
        /*
         * Remove everything
         */
        moveToAgain.stream().forEach(o -> assertTrue(qt.remove(TOKEN, o[0], o[1])));
        assertEquals(0, qt.query(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).size());
        moveToAgain.stream().forEach(o -> assertFalse(qt.remove(o, o[0], o[1])));
    }

    private static void testMove(
            final FlexibleQuadTree<Object> qt,
            final List<double[]> from,
            final List<double[]> to) {
        range().forEach(i -> assertTrue(qt.move(TOKEN, from.get(i), to.get(i))));
        assertEquals(INSERTIONS, qt.query(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE).size());
        from.stream().forEach(o -> assertFalse(qt.remove(TOKEN, o[0], o[1])));
    }

    private static IntStream range() {
        return IntStream.range(0, INSERTIONS);
    }

    private static List<double[]> makeRandomTest(final Random rnd) {
        return range()
                .mapToObj(i -> new double[] { rnd.nextLong() + rnd.nextDouble(), rnd.nextLong() + rnd.nextDouble() })
                .collect(Collectors.toList());
    }

    /**
     * 
     */
    @Test
    public void testSubdivide() {
        final SpatialIndex<Object> qt = new FlexibleQuadTree<>();
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS;
            qt.insert(v, val, val);
            qt.insert(v, -val, val);
            qt.insert(v, val, -val);
            qt.insert(v, -val, -val);
        });
        final double[] zz = new double[]{0, 0};
        final double[] minmin = new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE};
        final double[] maxmax = new double[]{Double.MAX_VALUE, Double.MAX_VALUE};
        final double[] minmax = new double[]{-Double.MAX_VALUE, Double.MAX_VALUE};
        final double[] maxmin = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
        assertEquals(4 * SUB_INS, qt.query(minmin, maxmax).size());
        assertEquals(SUB_INS + 3, qt.query(zz, maxmax).size());
        assertEquals(SUB_INS - 1, qt.query(zz, minmax).size());
        assertEquals(SUB_INS - 1, qt.query(zz, maxmin).size());
        assertEquals(SUB_INS - 1, qt.query(zz, minmin).size());
        final double halfWay = Math.nextDown(0.5);
        final double[] hminmin = new double[]{-halfWay, -halfWay};
        final double[] hmaxmax = new double[]{halfWay, halfWay};
        final double[] hminmax = new double[]{-halfWay, halfWay};
        final double[] hmaxmin = new double[]{halfWay, -halfWay};
        assertEquals(SUB_INS / 2, qt.query(hmaxmax, maxmax).size());
        assertEquals(SUB_INS / 2, qt.query(hminmax, minmax).size());
        assertEquals(SUB_INS / 2, qt.query(hmaxmin, maxmin).size());
        assertEquals(SUB_INS / 2, qt.query(hminmin, minmin).size());
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS;
            assertTrue("Test failed for " + v + ".", qt.move(v, new double[]{val, val}, new double[]{val / 2, val / 2}));
            assertTrue(qt.move(v, new double[]{-val, val}, new double[]{-val / 2, val / 2}));
            assertTrue(qt.move(v, new double[]{val, -val}, new double[]{val / 2, -val / 2}));
            assertTrue(qt.move(v, new double[]{-val, -val}, new double[]{-val / 2, -val / 2}));
        });
        assertEquals(4 * SUB_INS, qt.query(minmin, maxmax).size());
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS / 2;
            assertTrue(qt.remove(v, val, val));
            assertTrue(qt.remove(v, -val, val));
            assertTrue(qt.remove(v, val, -val));
            assertTrue(qt.remove(v, -val, -val));
        });
        assertEquals(0, qt.query(minmin, maxmax).size());
        IntStream.range(0, SUB_INS).forEach(v -> {
            final double val = v / (double) SUB_INS / 2;
            assertFalse(qt.remove(v, val, val));
            assertFalse(qt.remove(v, -val, val));
            assertFalse(qt.remove(v, val, -val));
            assertFalse(qt.remove(v, -val, -val));
        });
        assertEquals(0, qt.query(minmin, maxmax).size());
    }

    /**
     * This bug emerged during the experiments of Coordination 2016.
     */
    @Test
    public void testCoordinationBug() {
        final SpatialIndex<Object> qt = new FlexibleQuadTree<>();
        // CHECKSTYLE:OFF
        qt.insert(0, pos(0.1809460688778705, -0.47285587823182457));
        qt.insert(1, pos(-0.3486382251646362, 0.7869169418430895));
        qt.insert(2, pos(-0.7472564304558099, -0.6115874398653725));
        qt.insert(3, pos(-1.0055045893506822, -0.10851939793940944));
        qt.insert(4, pos(-0.39013219844649244, -0.8100882721969986));
        qt.insert(5, pos(0.33195444396622553, -0.7417396044345971));
        qt.insert(6, pos(0.8114170323852862, 0.39711305381369877));
        qt.insert(7, pos(-0.04705883426950412, -1.0340060571750946));
        qt.insert(8, pos(0.8218045713112644, -0.2066379106380759));
        qt.insert(9, pos(0.16650197698916022, -0.6162845456915751));
        qt.insert(10, pos(-0.2237599624200513, -0.09457854131077437));
        qt.insert(11, pos(-0.6185882157655296, -0.09403362967490554));
        qt.insert(12, pos(0.5628429819616689, -0.8349976615397777));
        qt.insert(13, pos(0.3007913184399999, 0.18474840335644088));
        qt.insert(14, pos(0.7269108136635913, 0.19018335694544597));
        qt.insert(15, pos(-0.3635629417153215, 0.07649114807133335));
        qt.insert(16, pos(0.011547580593794555, 0.1912122449628947));
        qt.move(0, pos(0.1809460688778705, -0.47285587823182457), pos(0.09528786508967618, -0.47516070619372375));
        assertTrue(qt.move(5, pos(0.33195444396622553, -0.7417396044345971), pos(0.4410930248572664, -0.8001956652807338)));
        assertTrue(qt.move(5, pos(0.4410930248572664, -0.8001956652807338), pos(0.3489312599527272, -0.779229474476143)));
        assertTrue(qt.move(12, pos(0.5628429819616689, -0.8349976615397777), pos(0.865804953660424, -0.5547419820867625)));
        assertTrue(qt.move(7, pos(-0.04705883426950412, -1.0340060571750946), pos(-0.08925077163021347, -1.0800567703586292)));
        assertTrue(qt.move(6, pos(0.8114170323852862, 0.39711305381369877), pos(0.5444437746419759, 0.7054373285196627)));
        assertTrue(qt.move(2, pos(-0.7472564304558099, -0.6115874398653725), pos(-0.6061479837028098, -0.6074099033077012)));
        assertTrue(qt.move(1, pos(-0.3486382251646362, 0.7869169418430895), pos(-0.10974719061666949, 0.2763607329920671)));
        assertTrue(qt.move(1, pos(-0.10974719061666949, 0.2763607329920671), pos(-0.020416611962305736, 0.48756041829877805)));
        assertTrue(qt.move(10, pos(-0.2237599624200513, -0.09457854131077437), pos(0.07881168992525472, -0.9577268277981856)));
        assertTrue(qt.move(14, pos(0.7269108136635913, 0.19018335694544597), pos(0.53514518514382, 0.17897202056863518)));
        assertTrue(qt.move(0, pos(0.09528786508967618, -0.47516070619372375), pos(0.24453135909475243, -0.29513241774829885)));
        assertTrue(qt.move(3, pos(-1.0055045893506822, -0.10851939793940944), pos(-1.0188609727428486, -0.01589508480830827)));
        assertTrue(qt.move(12, pos(0.865804953660424, -0.5547419820867625), pos(0.8923582713104145, -0.41861687992084606)));
        assertTrue(qt.move(4, pos(-0.39013219844649244, -0.8100882721969986), pos(-0.39898258146791793, -0.7607455862505399)));
        assertTrue(qt.move(6, pos(0.5444437746419759, 0.7054373285196627), pos(0.6718836390333339, 0.6326361806156805)));
        assertTrue(qt.move(7, pos(-0.08925077163021347, -1.0800567703586292), pos(1.0070506578623362, -0.13373491966739248)));
        assertTrue(qt.move(7, pos(1.0070506578623362, -0.13373491966739248), pos(1.0423552876544244, -0.08135464256624286)));
        // CHECKSTYLE:ON
    }

    private static double[] pos(final double x, final double y) {
        return new double[]{x, y};
    }
}
