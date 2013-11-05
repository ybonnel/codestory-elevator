/*
 * Copyright 2013- Yan Bonnel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.ybonnel.services;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class UpAndDownWithDirectionElevator extends CleverElevator {

    private static final Logger logger = LoggerFactory.getLogger(UpAndDownWithDirectionElevator.class);

    Direction currentDirection;

    private boolean mustReset = true;

    private Map<Direction, HashSet<Integer>> floorsToGo = new HashMap<Direction, HashSet<Integer>>(){{
        put(Direction.DOWN, new HashSet<Integer>());
        put(Direction.UP, new HashSet<Integer>());
    }};
    private Command lastCommand;

    public UpAndDownWithDirectionElevator() {
        reset("START");
    }


    private DescriptiveStatistics statsOfCall = new DescriptiveStatistics(500);

    public void logState() {
        logger.info("CurrentDirection : {}", currentDirection);
        logger.info("FloorsToGo for {} : [{}]", Direction.DOWN, floorsToGo.get(Direction.DOWN));
        logger.info("FloorsToGo for {} : [{}]", Direction.UP, floorsToGo.get(Direction.DOWN));
    }



    public boolean containsFloorForCurrentDirection() {
        for (int floor : floorsToGo.get(currentDirection)) {
            if (isFloorGoodForCurrentDirection(floor)) return true;
        }
        for (int floor : floorsToGo.get(currentDirection.getOtherDirection())) {
            if (isFloorGoodForCurrentDirection(floor)) return true;
        }
        return false;
    }

    private boolean isFloorGoodForCurrentDirection(int floor) {
        return currentDirection == Direction.UP
                && floor > currentFloor
                || currentDirection == Direction.DOWN
                && floor < currentFloor;
    }

    public boolean hasFloorsToGo() {
        return !floorsToGo.get(Direction.UP).isEmpty()
                || !floorsToGo.get(Direction.DOWN).isEmpty();
    }

    private int nbNothing = 0;

    @Override
    public Command nextCommand() {
        if (mustReset) {
            return Command.CLOSE;
        }
        lastCommand = getNextCommand();
        if (lastCommand == Command.NOTHING) {
            nbNothing++;
        } else {
            nbNothing = 0;
        }
        if (nbNothing > 18000) {
            mustReset = true;
        }
        return lastCommand;
    }

    private Command getNextCommand() {
        logState();
        if (hasFloorsToGo()) {
            if (isOpen()) {
                logger.info("Close doors");
                logState();
                return close();
            } else {
                if (openIfSomeoneWaiting() && lastCommand != Command.CLOSE) return openIfCan();
                if (!containsFloorForCurrentDirection()) {
                    currentDirection = currentDirection.getOtherDirection();
                }
                if (openIfSomeoneWaiting() && lastCommand != Command.CLOSE) return openIfCan();
                currentFloor += currentDirection.incForCurrentFloor;
                logState();
                return currentDirection.commandToGo;
            }
        } else {
            return goToBestFloorToWait();
        }
    }

    private Command goToBestFloorToWait() {
        if (isOpen()) {
            return close();
        }
        int bestFloorToWait = getBestFloorToWait();
        logger.info("Best floor to wait : {}", bestFloorToWait);
        if (currentFloor < bestFloorToWait) {
            return Command.UP;
        }
        if (currentFloor > bestFloorToWait) {
            return Command.DOWN;
        }
        return Command.NOTHING;
    }

    private int getBestFloorToWait() {
        if (statsOfCall.getN() > 0) {
            return (int) Math.round(statsOfCall.apply(new Median()));
        } else {
            return 0;
        }
    }

    private boolean openIfSomeoneWaiting() {
        if (floorsToGo.get(currentDirection).contains(currentFloor)) {
            floorsToGo.get(Direction.DOWN).remove(currentFloor);
            floorsToGo.get(Direction.UP).remove(currentFloor);
            logger.info("Open doors");
            logState();
            return true;
        }
        return false;
    }

    private void addCallToMap(int floor) {

        statsOfCall.addValue(floor);
    }

    @Override
    public void call(int floor, String to) {
        logState();
        addCallToMap(floor);
        if (floor != currentFloor || isClose()) {
            floorsToGo.get(Direction.valueOf(to)).add(floor);
        }
        logState();
    }

    @Override
    public void go(int floorToGo) {
        logState();
        if (floorToGo != currentFloor || isClose()) {
            floorsToGo.get(Direction.DOWN).add(floorToGo);
            floorsToGo.get(Direction.UP).add(floorToGo);
        }
        logState();
    }

    @Override
    public void userHasEntered() {
    }

    @Override
    public void userHasExited() {
    }

    @Override
    public void reset(String cause) {
        super.reset(cause);
        if (!"START".equals(cause)) {
            mustReset = false;
        }
        statsOfCall.clear();
        currentDirection = Direction.UP;
        floorsToGo.get(Direction.DOWN).clear();
        floorsToGo.get(Direction.UP).clear();
    }
}
