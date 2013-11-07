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

import java.util.HashSet;

public class FastDeliverElevator extends CleverElevator {

    private static final Logger logger = LoggerFactory.getLogger(FastDeliverElevator.class);


    private HashSet<Integer> floorsToGo = new HashSet<>();
    private HashSet<Integer> floorsHasCalled = new HashSet<>();

    public FastDeliverElevator() {
    }



    public void logState() {
        logger.info("CurrentFloor : {}", currentFloor);
        logger.info("FloorsToGo : {}", floorsToGo);
        logger.info("FllorsHasCalled : {}", floorsHasCalled);
    }

    private int getNearestFloorFromOneStack(HashSet<Integer> floors) {
        int minDiff = 999;
        int minFloor = 0;
        for (int floor : floors) {
            if (Math.abs(floor - currentFloor) < minDiff) {
                minFloor = floor;
                minDiff = Math.abs(floor - currentFloor);
            }
        }
        return minFloor;
    }

    private int getNearestFloor() {
        if (!floorsToGo.isEmpty()) {
            return getNearestFloorFromOneStack(floorsToGo);
        }
        return getNearestFloorFromOneStack(floorsHasCalled);
    }

    @Override
    protected Command getNextCommand() {
        if (floorsToGo.isEmpty() && floorsHasCalled.isEmpty()) {
            return goToBestFloorToWait();
        } else {
            if (isOpen()) {
                return close();
            } else {
                int floorToGo = getNearestFloor();
                logger.info("Floor to go : {}", floorToGo);
                logState();

                if (floorToGo == currentFloor) {
                    floorsToGo.remove(currentFloor);
                    floorsHasCalled.remove(currentFloor);
                    return openIfCan();
                }
                Direction direction = floorToGo < currentFloor ? Direction.DOWN : Direction.UP;

                currentFloor += direction.incForCurrentFloor;
                return direction.commandToGo;
            }
        }
    }

    @Override
    public void addCall(int floor, String to) {
        floorsHasCalled.add(floor);
    }

    @Override
    public void go(int floorToGo) {
        floorsToGo.add(floorToGo);
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
        floorsToGo.clear();
        floorsHasCalled.clear();
    }
}
