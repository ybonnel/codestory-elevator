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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class UpAndDownWithDirectionElevator extends CleverElevator {

    private static final Logger logger = LoggerFactory.getLogger(UpAndDownWithDirectionElevator.class);

    Direction currentDirection = Direction.UP;

    private Map<Direction, HashSet<Integer>> floorsToGo = new HashMap<Direction, HashSet<Integer>>(){{
        put(Direction.DOWN, new HashSet<Integer>());
        put(Direction.UP, new HashSet<Integer>());
    }};

    public void logState() {
        logger.info("CurrentDirection : {}, FloorsToGo for UP : {}, FloorsToGo for DOWN : {}",
                currentDirection,
                floorsToGo.get(Direction.DOWN),
                floorsToGo.get(Direction.UP));
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

    @Override
    protected Command getNextCommand() {
        logState();
        if (hasFloorsToGo()) {
            if (isOpen()) {
                logger.info("Close doors");
                return close();
            } else {
                if (openIfSomeoneWaiting() && lastCommand != Command.CLOSE) return openIfCan();
                if (!containsFloorForCurrentDirection()) {
                    currentDirection = currentDirection.getOtherDirection();
                }
                if (openIfSomeoneWaiting() && lastCommand != Command.CLOSE) return openIfCan();
                currentFloor += currentDirection.incForCurrentFloor;
                return currentDirection.commandToGo;
            }
        } else {
            return goToBestFloorToWait();
        }
    }

    private boolean openIfSomeoneWaiting() {
        if (floorsToGo.get(currentDirection).contains(currentFloor)) {
            floorsToGo.get(Direction.DOWN).remove(currentFloor);
            floorsToGo.get(Direction.UP).remove(currentFloor);
            logger.info("Open doors");
            return true;
        }
        return false;
    }


    @Override
    protected void addCall(int floor, String to) {
        logState();
        if (floor != currentFloor || isClose()) {
            floorsToGo.get(Direction.valueOf(to)).add(floor);
        }
    }

    @Override
    public void go(int floorToGo) {
        logState();
        if (floorToGo != currentFloor || isClose()) {
            floorsToGo.get(Direction.DOWN).add(floorToGo);
            floorsToGo.get(Direction.UP).add(floorToGo);
        }
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
        currentDirection = Direction.UP;
        floorsToGo.get(Direction.DOWN).clear();
        floorsToGo.get(Direction.UP).clear();
    }
}
