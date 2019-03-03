/*
 * LEAPS - Low Energy Accurate Positioning System.
 *
 * Copyright (c) 2016-2017, LEAPS. All rights reserved.
 */

package com.decawave.argomanager.components.impl;

import com.decawave.argo.api.struct.Distance;
import com.decawave.argo.api.struct.Position;
import com.decawave.argomanager.Constants;
import com.decawave.argomanager.components.struct.ComputedPosition;
import com.decawave.argomanager.components.struct.NodeDistanceMatrix;
import com.decawave.argomanager.util.Util;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Argo project.
 */

public class AutoPositioningAlgorithm {

    private static final int MINIMAL_TRILATERAL_NODE_DISTANCE = 500;
    private static final double ONE_DEGREE = Math.PI / 180;
    private static final double MINIMAL_THIRD_TRILATERAL_POINT_ANGLE = 45 * ONE_DEGREE;

    public static class Result {
        public final ResultCode code;
        public final Map<Long, ComputedPosition> positions;

        Result(ResultCode code) {
            if (Constants.DEBUG) {
                Preconditions.checkState(code != ResultCode.SUCCESS);
            }
            this.code = code;
            this.positions = null;
        }

        Result(ResultCode code, Map<Long, ComputedPosition> positions) {
            this.code = code;
            this.positions = positions;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Result{code=").append(code).append(", positions=");
            if (positions == null) {
                sb.append("null");
            } else {
                sb.append('{');
                for (Map.Entry<Long, ComputedPosition> entry : positions.entrySet()) {
                    sb.append(Util.shortenNodeId(entry.getKey(), false))
                            .append("=")
                            .append(entry.getValue());
                }
                sb.append('}');
            }
            sb.append("}");
            return sb.toString();
        }
    }

    public static Result computePositions(
            ArrayList<Long> nodeOrder,
            NodeDistanceMatrix distanceMatrix,
            int uniformZAxis) {
        int ctr = 0;
        distanceMatrix = distanceMatrix.toLongDistanceMatrix(nodeOrder);
        Map<Long, ComputedPosition> positionMap = new HashMap<>();
        for (Long nodeId : nodeOrder) {
            ComputedPosition computedPosition = new ComputedPosition();
            switch (ctr++) {
                case 0:
                    computedPosition.position = new Position(0, 0, 0, (byte) 100);
                    break;
                case 1:
                    // trivial: retrieve the x shift
                    Long zeroNode = nodeOrder.get(0);
                    Distance distance = distanceMatrix.evaluateDistance(zeroNode, nodeId);
                    if (distance == null) {
                        return new Result(ResultCode.MISSING_DISTANCE_0_TO_1);
                    }
                    computedPosition.position = new Position(distance.length, 0, 0,
                            // and the quality
                            combineQuality(positionMap.get(zeroNode).position.qualityFactor, distance.quality));
                    computedPosition.fromNodes[0] = nodeOrder.get(0);
                    break;
                case 2:
                    Distance d1 = distanceMatrix.evaluateDistance(nodeOrder.get(0), nodeId);
                    Distance d2 = distanceMatrix.evaluateDistance(nodeOrder.get(1), nodeId);
                    if (d1 == null) {
                        return new Result(ResultCode.MISSING_DISTANCE_0_TO_2);
                    }
                    if (d2 == null) {
                        return new Result(ResultCode.MISSING_DISTANCE_1_TO_2);
                    }
                    int r1 = d1.length;
                    int r2 = d2.length;
                    // get the x offset of the second node
                    ComputedPosition firstNodePosition = positionMap.get(nodeOrder.get(0));
                    ComputedPosition secondNodePosition = positionMap.get(nodeOrder.get(1));
                    int d = secondNodePosition.position.x;
                    int i = (int) ((Math.pow(r1, 2) + Math.pow(d, 2) - Math.pow(r2, 2)) / (2 * d) + 0.5);
                    int j = (int) (Math.sqrt(Math.pow(r2, 2) - Math.pow(d - i, 2)) + 0.5);
                    // compute quality
                    computedPosition.position = new Position(i, j, 0,
                            combineQuality(firstNodePosition.position.qualityFactor, secondNodePosition.position.qualityFactor, d1.quality, d2.quality));
                    if (!checkPositionOrthogonality(firstNodePosition.position, secondNodePosition.position, computedPosition.position, MINIMAL_THIRD_TRILATERAL_POINT_ANGLE)) {
                        return new Result(ResultCode.DRIVING_NODES_NOT_ORTHOGONAL_ENOUGH);
                    }
                    computedPosition.fromNodes[0] = nodeOrder.get(0);
                    computedPosition.fromNodes[1] = nodeOrder.get(1);
                    break;
                default:
                    // and the rest
                    // get the first three distances
                    Long[] inputNodes = new Long[3];
                    Distance distances[] = new Distance[3];
                    Position inputPositions[] = new Position[3];
                    // go through the computed node positions one-by-one (prefer the ones computed earliest - minimize systematic error)
                    double minimalTrilateralOrthogonalAngle = MINIMAL_THIRD_TRILATERAL_POINT_ANGLE;
                    int ai;
                    int attemptCounter = 0;
                    boolean success = false;
                    while (attemptCounter++ < 3) {
                        ai = 0;
                        a:
                        for (int k = 0; k < ctr; k++) {
                            Long candidateNodeId = nodeOrder.get(k);
                            Distance dist = distanceMatrix.evaluateDistance(candidateNodeId, nodeId);
                            ComputedPosition cPos = positionMap.get(candidateNodeId);
                            if (dist != null && cPos.success) {
                                // we need to have distant enough points
                                for (int ii = ai - 1; ii >= 0; ii--) {
                                    // we need to have distant enough points
                                    if (Util.nodeDistance(inputPositions[ii], cPos.position) < MINIMAL_TRILATERAL_NODE_DISTANCE) {
                                        continue a;
                                    }
                                }
                                // if this is the third node, we need to check that the nodes do not lie on a line
                                if (ai == 2) {
                                    if (!checkPositionOrthogonality(inputPositions[0], inputPositions[1], cPos.position, minimalTrilateralOrthogonalAngle))
                                        continue;
                                }
                                // we know the distance from a node whose position has been successfully computed
                                inputNodes[ai] = candidateNodeId;
                                distances[ai] = dist;
                                inputPositions[ai] = cPos.position;
                                ai++;
                                if (ai >= distances.length) {
                                    // we've got the necessary distances
                                    break;
                                }
                            }
                        }
                        if (ai == 3) {
                            success = true;
                            break;
                        } // else:
                        minimalTrilateralOrthogonalAngle /= 2;
                    }
                    if (success) {
                        // we have enough input nodes, but we need to normalize them:
                        // 1. first node in 0,0             (solve with offset)
                        // 2. second node in <distance>,0   (solve with rotation)

                        // solve shift/offset
                        final int xOffset = inputPositions[0].x;
                        final int yOffset = inputPositions[0].y;
                        int x1 = inputPositions[1].x - xOffset;
                        int y1 = inputPositions[1].y - yOffset;
                        i = inputPositions[2].x - xOffset;
                        j = inputPositions[2].y - yOffset;

                        // rotation
                        double rotationAngle = 0;
                        if (y1 != 0) {
                            // we need to tweak the x and y axis
                            rotationAngle = computeAngle(x1, y1);
                            // y1 = 0;
                            // this is equal to distance
                            x1 = Util.nodeDistance(inputPositions[0], inputPositions[1]);
                            // now rotate the third point
                            int d0to2 = Util.nodeDistance(inputPositions[0], inputPositions[2]);
                            //
                            double originalRotation = computeAngle(i, j);
                            double diffRotation = originalRotation - rotationAngle;
                            double scaleXFactor = Math.cos(diffRotation);
                            double scaleYFactor = Math.sin(diffRotation);
                            i = (int) (d0to2 * scaleXFactor + 0.5);
                            j = (int) (d0to2 * scaleYFactor + 0.5);
                        }

                        // extract distances
                        r1 = distances[0].length;
                        r2 = distances[1].length;
                        int r3 = distances[2].length;
                        // the math below works for the first node in 0,0 and second node in <distance>,0
                        d = x1;
                        int x = (int) ((Math.pow(r1, 2) + Math.pow(d, 2) - Math.pow(r2, 2)) / (2 * d) + 0.5);
                        int y = (int) ((Math.pow(r1, 2) - Math.pow(r3, 2) + Math.pow(i, 2) + Math.pow(j, 2)) / (2 * j) - x * i / j + 0.5);
                        // now transform the computed coordinates back
                        if (rotationAngle != 0) {
                            int computedDistance = Util.distance(x, y);
                            double angle = computeAngle(x, y);
                            // rotate back
                            angle += rotationAngle;
                            //
                            x = (int) (Math.cos(angle) * computedDistance + 0.5);
                            y = (int) (Math.sin(angle) * computedDistance + 0.5);
                        }
                        if (xOffset != 0) {
                            x += xOffset;
                        }
                        if (yOffset != 0) {
                            y += yOffset;
                        }
                        // finally save the result
                        computedPosition.position = new Position(x, y, 0,
                                combineQuality(inputPositions[0].qualityFactor, inputPositions[1].qualityFactor, inputPositions[2].qualityFactor,
                                        distances[0].quality, distances[1].quality, distances[2].quality));
                        computedPosition.fromNodes = inputNodes;
                    } else {
                        computedPosition.success = false;
                    }
                    break;
            }
            if (computedPosition.success) {
                // decorate the computed position with uniform z-axis value
                computedPosition.position.z = uniformZAxis;
            }
            positionMap.put(nodeId, computedPosition);
        }
        // the change event will (MUST) be raised by upper layer
        return new Result(ResultCode.SUCCESS, positionMap);
    }

    private static boolean checkPositionOrthogonality(Position firstPosition, Position secondPosition, Position thirdPosition, double minimalThirdTrilateralPointAngle) {
        // avoid division by zero
        int yDiff1 = firstPosition.y - secondPosition.y;
        int xDiff1 = firstPosition.x - secondPosition.x;
        int yDiff2 = secondPosition.y - thirdPosition.y;
        int xDiff2 = secondPosition.x - thirdPosition.x;
        if (xDiff1 == 0 && xDiff2 == 0) {
            // skip this one
            return false;
        }
        if (xDiff1 != 0 && xDiff2 != 0) {
            float k1 = (1f * yDiff1) / xDiff1;
            float k2 = (1f * yDiff2) / xDiff2;
            // check the angle difference
            if (Math.abs(Math.atan(k1) - Math.atan(k2)) < minimalThirdTrilateralPointAngle) {
                // skip this one
                return false;
            }
        }
        // the nodes are orthogonal enough
        return true;
    }

    private static double computeAngle(int x1, int y1) {
        double angle;
        if (y1 == 0) {
            angle = 0;
        } else if (x1 == 0) {
            angle = y1 > 0 ? Math.PI / 2 : - Math.PI / 2;
        } else {
            // this returns result in range (- pi/2 ; pi/2 )
            angle = Math.atan(1.0 * y1 / x1);
            if (x1 < 0) {
                angle = Math.PI + angle;
            }
        }
        if (angle < 0) {
            // always return a positive result
            angle += 2 * Math.PI;
        }
        return angle;
    }


    public enum ResultCode {
        SUCCESS,
        MISSING_DISTANCE_0_TO_1,
        MISSING_DISTANCE_1_TO_2,
        MISSING_DISTANCE_0_TO_2,
        DRIVING_NODES_NOT_ORTHOGONAL_ENOUGH,
    }

    /**
     * If no quality is given, 100 is returned.
     */
    private static byte combineQuality(byte... qualities) {
        float q = 1;
        for (byte quality : qualities) {
            if (Constants.DEBUG) {
                Preconditions.checkState(quality <= 100, "quality cannot be greater than 100: " + quality);
            }
            q *= (quality / 100f);
        }
        return (byte) (q * 100);
    }

}
