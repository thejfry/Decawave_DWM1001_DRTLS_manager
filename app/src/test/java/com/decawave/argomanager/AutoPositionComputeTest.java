/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager;

import com.decawave.argo.api.struct.Distance;
import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.components.impl.AutoPositioningAlgorithm;
import com.decawave.argomanager.components.struct.ComputedPosition;
import com.decawave.argomanager.components.struct.NodeDistanceMatrix;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class AutoPositionComputeTest {
    private static final int TEST_NN_COUNT = 16;
    private static final int MAX_SPREAD_MM = 1000;
    private static final boolean DEBUG = true;
    private static final int ALLOWED_DEVIATION_SIMPLE = 2;          // 2 mm
    private static final int ALLOWED_DEVIATION_DOUBLE_LEVEL = 10;   // 1 cm


    @Test
    public void testIdealAutoPositionCompute() {
        basicAutoPositionScenario(false, false);
    }

    // TODO: this test needs a fix
    public void testLowHiQualityDistanceAutoPositionCompute() {
        basicAutoPositionScenario(true, false);
    }

    // TODO: this test needs a fix
    public void testDoubleLevelDistancesAutoPositionCompute() {
        // the node number 9 has only distances from nodes 6,7,8 which lie 'on the same line'
        // therefore computation of position of this node fails
        basicAutoPositionScenario(false, true, 9);
    }

    @Test
    public void testJirkaOrthogonality() {
        NodeDistanceMatrix distanceMatrix = new NodeDistanceMatrix();
        createOrthogonalityTestData(distanceMatrix);
        System.out.println("distance matrix is: " + distanceMatrix);
        // now compute
        ArrayList<Long> nodeIds = Lists.newArrayList(
                0x115EL,
                0x1125L,
                0x1151L,
                0x1121L,
                0x0CA8L,
                0x1150L,
                0x116EL,
                0x111CL);
        AutoPositioningAlgorithm.Result result = AutoPositioningAlgorithm.computePositions(nodeIds, distanceMatrix, 0);
        assertTrue("auto position compute result: " + result.code, result.code == AutoPositioningAlgorithm.ResultCode.SUCCESS);
    }

    private void basicAutoPositionScenario(boolean fillWithInverseLowQualityDistances, boolean doubleLevelDistanceMatrix, long... failNodeIds) {
        // create sample test positions
        Map<Long, Position> positions = new HashMap<>();
        ArrayList<Long> nodeIds = new ArrayList<>(TEST_NN_COUNT);
        NodeDistanceMatrix distanceMatrix = new NodeDistanceMatrix();
        createTestData(40000, 60000, nodeIds, positions, distanceMatrix, fillWithInverseLowQualityDistances, doubleLevelDistanceMatrix);
        // now compute the positions
        AutoPositioningAlgorithm.Result result = AutoPositioningAlgorithm.computePositions(nodeIds, distanceMatrix, 0);
        assertTrue("auto position compute result: " + result.code, result.code == AutoPositioningAlgorithm.ResultCode.SUCCESS);
        // check the computed positions
        System.out.println("=== COMPUTED POSITIONS ===");
        dumpPositions(nodeIds, result.positions);
        // check that the computed positions correspond to those in input
        checkPositionsMatch(positions, result.positions, doubleLevelDistanceMatrix ? ALLOWED_DEVIATION_DOUBLE_LEVEL : ALLOWED_DEVIATION_SIMPLE, failNodeIds);
    }

    private void checkPositionsMatch(Map<Long, Position> positions, Map<Long, ComputedPosition> cPositions, int allowedDeviation, long... failNodeIds) {
        Set<Long> failNodeSet = new HashSet<>();
        for (Long aLong : failNodeIds) {
            failNodeSet.add(aLong);
        }
        for (Map.Entry<Long, Position> origPosition : positions.entrySet()) {
            Long nodeId = origPosition.getKey();
            ComputedPosition computedPosition = cPositions.get(nodeId);
            assertNotNull("there is no position computed for " + nodeId, computedPosition);
            if (failNodeSet.contains(nodeId)) {
                assertFalse("the position for " + nodeId + " shouldn't get computed", computedPosition.success);
            } else {
                assertTrue("the position for " + nodeId + " wasn't successfully computed", computedPosition.success);
                checkPositionMatch(nodeId, origPosition.getValue(), computedPosition, allowedDeviation);
            }
        }
    }


    private void checkPositionMatch(long nodeId, Position origPositionValue, ComputedPosition computedPosition, int allowedDeviation) {
        Preconditions.checkNotNull(origPositionValue);
        Preconditions.checkNotNull(computedPosition);
        // tolerance: 3 mm
        Assert.assertTrue("computed position of node " + nodeId + " x-coordinate does not match: " + origPositionValue + ", computed = " + computedPosition.position,
                Math.abs(origPositionValue.x - computedPosition.position.x) <= allowedDeviation);
        Assert.assertTrue("computed position of node " + nodeId + " y-coordinate does not match: " + origPositionValue + ", computed = " + computedPosition.position,
                Math.abs(origPositionValue.y - computedPosition.position.y) <= allowedDeviation);
    }

    private void createOrthogonalityTestData(NodeDistanceMatrix distanceMatrix) {
        d(distanceMatrix, 0x1121, 0x1125, 4095,44);
        d(distanceMatrix, 0x1121, 0x116E, 4939,54);
        d(distanceMatrix, 0x1121, 0x115E, 6313,54);
        d(distanceMatrix, 0x1121, 0x0CA8, 7731,54);
        d(distanceMatrix, 0x1121, 0x1151, 6268,20);
        d(distanceMatrix, 0x1121, 0x111C, 2014,54);
        d(distanceMatrix, 0x1121, 0x1150, 5233,54);

        d(distanceMatrix, 0x1151, 0x1125, 2005,54);
        d(distanceMatrix, 0x1151, 0x116E, 7694,54);
        d(distanceMatrix, 0x1151, 0x115E, 5242,54);
        d(distanceMatrix, 0x1151, 0x1121, 6267,12);
        d(distanceMatrix, 0x1151, 0x0CA8, 5046,54);
        d(distanceMatrix, 0x1151, 0x111C, 7944,54);
        d(distanceMatrix, 0x1151, 0x1150, 9239,54);

        d(distanceMatrix, 0x0CA8, 0x1125, 5398,0);
        d(distanceMatrix, 0x0CA8, 0x116E, 5883,54);
        d(distanceMatrix, 0x0CA8, 0x115E, 2000,54);
        d(distanceMatrix, 0x0CA8, 0x1121, 7734,54);
        d(distanceMatrix, 0x0CA8, 0x1151, 5039,54);
        d(distanceMatrix, 0x0CA8, 0x111C, 9252,54);
        d(distanceMatrix, 0x0CA8, 0x1150, 7815,52);

        d(distanceMatrix, 0x1150, 0x1125, 7628,54);
        d(distanceMatrix, 0x1150, 0x116E, 1736,4);
        d(distanceMatrix, 0x1150, 0x115E, 5904,98);
        d(distanceMatrix, 0x1150, 0x1121, 5236,54);
        d(distanceMatrix, 0x1150, 0x0CA8, 7814,53);
        d(distanceMatrix, 0x1150, 0x1151, 9251,54);
        d(distanceMatrix, 0x1150, 0x111C, 5208,0);

        d(distanceMatrix, 0x116E, 0x1125, 6236,98);
        d(distanceMatrix, 0x116E, 0x115E, 3946,98);
        d(distanceMatrix, 0x116E, 0x1121, 4942,96);
        d(distanceMatrix, 0x116E, 0x0CA8, 5878,98);
        d(distanceMatrix, 0x116E, 0x1151, 7700,98);
        d(distanceMatrix, 0x116E, 0x111C, 5254,96);
        d(distanceMatrix, 0x116E, 0x1150, 1761,0);

        d(distanceMatrix, 0x1125, 0x116E, 6238,54);
        d(distanceMatrix, 0x1125, 0x115E, 4687,54);
        d(distanceMatrix, 0x1125, 0x1121, 4102,46);
        d(distanceMatrix, 0x1125, 0x0CA8, 5379,0);
        d(distanceMatrix, 0x1125, 0x1151, 1998,54);
        d(distanceMatrix, 0x1125, 0x111C, 5973,52);
        d(distanceMatrix, 0x1125, 0x1150, 7620,54);

        d(distanceMatrix, 0x115E, 0x1125, 4695,98);
        d(distanceMatrix, 0x115E, 0x116E, 3949,98);
        d(distanceMatrix, 0x115E, 0x1121, 6313,98);
        d(distanceMatrix, 0x115E, 0x0CA8, 2002,98);
        d(distanceMatrix, 0x115E, 0x1151, 5235,98);
        d(distanceMatrix, 0x115E, 0x111C, 7645,98);
        d(distanceMatrix, 0x115E, 0x1150, 5899,98);

        d(distanceMatrix, 0x111C, 0x1125, 5976,98);
        d(distanceMatrix, 0x111C, 0x116E, 5249,98);
        d(distanceMatrix, 0x111C, 0x115E, 7639,98);
        d(distanceMatrix, 0x111C, 0x1121, 2020,98);
        d(distanceMatrix, 0x111C, 0x0CA8, 9255,98);
        d(distanceMatrix, 0x111C, 0x1151, 7942,98);
        d(distanceMatrix, 0x111C, 0x1150, 5163,0);
    }

    private static void d(NodeDistanceMatrix distanceMatrix, long fromNode, long toNode, int distance, int quality) {
        distanceMatrix.putDistance(fromNode, toNode, new Distance(distance, (byte) quality));
    }

    private void createTestData(int width, int height, ArrayList<Long> nodeIdsOut, Map<Long, Position> positionsOut,
                                NodeDistanceMatrix distanceMatrix,
                                boolean fillDistanceMatrixWithLowQualityDistances, boolean configureDoubleLevelDistances) {
        Random random = new Random(234235);
        // generate positions
        generatePositions(width, height, positionsOut, random);
        // now generate the order
        //noinspection ConstantConditions
        nodeIdsOut.addAll(Lists.transform(Arrays.asList(1, 3, 11, 2, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16), (i) -> (long) i));
        // generate the distance matrix
        if (configureDoubleLevelDistances) {
            configureDoubleLevelDistanceMatrix(distanceMatrix, positionsOut, 1, 3, 11, random);
        } else {
            configureDistanceMatrix(distanceMatrix, positionsOut, 1L, 3L, 11L);
        }
        //
        if (fillDistanceMatrixWithLowQualityDistances) {
            // now fill distance matrix also with other position values with lower quality
            fillDistanceMatrixWithInverseLowQualityDistances(distanceMatrix, random);
        }
        if (DEBUG) {
            dumpTestData(nodeIdsOut, positionsOut, distanceMatrix);
        }
    }

    private void configureDoubleLevelDistanceMatrix(NodeDistanceMatrix distanceMatrix, Map<Long, Position> positionsOut, int node1, int node2, int node3, Random random) {
        int edge = positionsOut.size() / 2;
        int ctr = 0;
        ArrayList<Long> distanceFrom123 = new ArrayList<>();
        for (Map.Entry<Long, Position> entry : positionsOut.entrySet()) {
            Long nodeId = entry.getKey();
            if (nodeId == node2) {
                setDistanceFromPosition(distanceMatrix, node1, node2, positionsOut);
            } else if (nodeId == node3) {
                setDistanceFromPosition(distanceMatrix, node1, node3, positionsOut);
                setDistanceFromPosition(distanceMatrix, node2, node3, positionsOut);
            } else if (nodeId != node1) {
                long n1, n2, n3;
                if (ctr < edge) {
                    // set distances from 1,3 and 11
                    n1 = node1;
                    n2 = node2;
                    n3 = node3;
                    distanceFrom123.add(nodeId);
                } else {
                    // set random distances from nodes in 123 (those which have their position based on computation from 1,2,3)
                    int size123 = distanceFrom123.size();
                    int idx1 = (int) (random.nextFloat() * size123);
                    int idx2;
                    do {
                        idx2 = (int) (random.nextFloat() * size123);
                    } while (idx2 == idx1);
                    int idx3;
                    do {
                        idx3 = (int) (random.nextFloat() * size123);
                    } while (idx3 == idx1 || idx3 == idx2);
                    //
                    n1 = distanceFrom123.get(idx1);
                    n2 = distanceFrom123.get(idx2);
                    n3 = distanceFrom123.get(idx3);
                }
                setDistanceFromPosition(distanceMatrix, n1, nodeId, positionsOut);
                setDistanceFromPosition(distanceMatrix, n2, nodeId, positionsOut);
                setDistanceFromPosition(distanceMatrix, n3, nodeId, positionsOut);
            }
            ctr++;
        }
    }

    private void fillDistanceMatrixWithInverseLowQualityDistances(NodeDistanceMatrix distanceMatrix, Random random) {
        int i = 0;
        Set<Long> sourceNodes = new HashSet<>(distanceMatrix.getSourceNodes());
        for (Long sourceNode : sourceNodes) {
            Set<Long> targetNodes = new HashSet<>(distanceMatrix.getTargetNodes(sourceNode));
            for (Long targetNode : targetNodes) {
                // check if there is already the inverse distance
                if (distanceMatrix.hasDistanceInDirection(targetNode, sourceNode)) {
                    // we will not override existing distance
                    continue;
                }
                // put the inverse distance
                if (i++ % 2 == 0) {
                    // skip this one
                    continue;
                }
                Distance distance = distanceMatrix.evaluateDistance(sourceNode, targetNode);
                byte quality = (byte) (distance.quality - 20);
                assertTrue("original quality is too low!: " + distance.quality, distance.quality > quality);
                distanceMatrix.putDistance(targetNode, sourceNode, new Distance((int) (distance.length + (random.nextFloat() - 0.5) * 4000), quality));
            }
        }
    }

    private void dumpTestData(ArrayList<Long> nodeIdsOut, Map<Long, Position> positionsOut, NodeDistanceMatrix distanceMatrix) {
        System.out.println("generated test data:");
        System.out.println("=== POSITIONS ===");
        dumpPositions(nodeIdsOut, positionsOut);
        System.out.println("=== DISTANCES ===");
        System.out.println(distanceMatrix);
    }

    private void generatePositions(int width, int height, Map<Long, Position> positionsOut, Random random) {
        float wStep = width / 3f;
        float hStep = height / 3f;
        int wc = 0, hc = 0;
        // generate positions
        for (int id = 1; id <= TEST_NN_COUNT; id++) {
            int hDeviation = 0;
            int vDeviation = 0;
            if (id != 1) {
                // we need the first node to be in 0,0
                if (id != 3) {
                    // we need the third point to be exactly on x axis (zero y axis: vDeviation = 0)
                    vDeviation = (int) (MAX_SPREAD_MM * (random.nextFloat() - 0.5));
                }
                hDeviation = (int) (MAX_SPREAD_MM * (random.nextFloat() - 0.5));
            }
            positionsOut.put((long) id, new Position((int) (wc * wStep + hDeviation), (int) (hc * hStep + vDeviation), 0));
            wc++;
            if (wc == 4) {
                wc = 0;
                hc++;
            }
        }
    }

    private void configureDistanceMatrix(NodeDistanceMatrix distanceMatrix, Map<Long, Position> positions,
                                         long node1, long node2, long node3) {
        for (Map.Entry<Long, Position> entry : positions.entrySet()) {
            Long nodeId = entry.getKey();
            if (nodeId == node2) {
                setDistanceFromPosition(distanceMatrix, node1, node2, positions);
            } else if (nodeId == node3) {
                setDistanceFromPosition(distanceMatrix, node1, node3, positions);
                setDistanceFromPosition(distanceMatrix, node2, node3, positions);
            } else if (nodeId != node1) {
                // set distances from 1,3 and 11
                setDistanceFromPosition(distanceMatrix, node1, nodeId, positions);
                setDistanceFromPosition(distanceMatrix, node2, nodeId, positions);
                setDistanceFromPosition(distanceMatrix, node3, nodeId, positions);
            }
        }
    }

    private void dumpPositions(ArrayList<Long> nodeIdsOut, Map<Long, ?> positionsOut) {
        for (Long nodeId : nodeIdsOut) {
            System.out.println("node " + nodeId + ": " + positionsOut.get(nodeId));
        }
    }

    private void setDistanceFromPosition(NodeDistanceMatrix distanceMatrix, long fromNode, long toNode, Map<Long, Position> positions) {
        int d = positionDistance(positions.get(fromNode), positions.get(toNode));
        distanceMatrix.putDistance(fromNode, toNode, new Distance(d, (byte) 80));
    }

    private static int positionDistance(Position p1, Position p2) {
        return distance(p2.x - p1.x, p2.y - p1.y);
    }

    private static int distance(int x, int y) {
        return (int) (Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)) + 0.5);
    }


}