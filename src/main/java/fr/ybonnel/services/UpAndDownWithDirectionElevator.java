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

public class UpAndDownWithDirectionElevator extends CleverElevator {

    private final Logger logger;

    Direction currentDirection = Direction.UP;

    private IFloorsByDirection floorsByDirection;

    public UpAndDownWithDirectionElevator() {
        logger = LoggerFactory.getLogger(getClass());
        floorsByDirection = new FloorsByDirection();
    }

    protected UpAndDownWithDirectionElevator(IFloorsByDirection floorsByDirection) {
        logger = LoggerFactory.getLogger(getClass());
        this.floorsByDirection = floorsByDirection;
    }

    public void logState() {
        logger.info("CurrentDirection : {}, FloorsByDirection : {}",
                currentDirection,
                floorsByDirection);
    }



    public boolean hasFloorsToGo() {
        return !floorsByDirection.isEmpty();
    }

    @Override
    protected Command getNextCommand() {
        logState();
        floorsByDirection.nextCommandCalled();
        if (hasFloorsToGo()) {
            if (isOpen()) {
                logger.info("Close doors");
                return close();
            } else {
                if (floorsByDirection.mustOpenFloorForThisDirection(currentFloor, currentDirection)
                        && lastCommand != Command.CLOSE) {
                    return openIfCan();
                }

                if (!floorsByDirection.containsFloorForCurrentDirection(currentFloor, currentDirection)) {
                    currentDirection = currentDirection.getOtherDirection();
                }

                if (floorsByDirection.mustOpenFloorForThisDirection(currentFloor, currentDirection)
                        && lastCommand != Command.CLOSE) {
                    return openIfCan();
                }

                currentFloor += currentDirection.incForCurrentFloor;
                return currentDirection.commandToGo;
            }
        } else {
            return goToBestFloorToWait();
        }
    }

    @Override
    protected Command openIfCan() {
        Command command = super.openIfCan();
        if (command == Command.OPEN) {
            floorsByDirection.willOpenDoorsOnFloor(currentFloor);
        }
        return command;
    }


    @Override
    protected void addCall(int floor, String to) {
        logState();
        if (floor != currentFloor || isClose()) {
            floorsByDirection.addFloorForDirection(floor, Direction.valueOf(to));
        }
    }

    @Override
    public void go(int floorToGo) {
        logState();
        if (floorToGo != currentFloor || isClose()) {
            floorsByDirection.addFloorToGo(floorToGo);
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
        floorsByDirection.clear();
    }
}
