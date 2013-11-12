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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AlzheimerFloorsByDirection implements IFloorsByDirection {

    private int getNbMaxWait() {
        return 20;
    }

    public void setLowerFloor(Integer lowerFloor) {
    }

    public void setHigherFloor(Integer higherFloor) {
    }

    private Map<Direction, Map<Integer, Integer>> floorsHasCalled = new HashMap<Direction, Map<Integer, Integer>>() {{
        put(Direction.DOWN, new HashMap<Integer, Integer>());
        put(Direction.UP, new HashMap<Integer, Integer>());
    }};

    private Map<Integer, Integer> floorsToGo = new HashMap<>();

    private Map<Integer, Integer> lastWaitTimeBeforeOpen = new HashMap<>();

    @Override
    public String toString() {
        return floorsToGo.toString();
    }

    public void clear() {
        floorsHasCalled.get(Direction.DOWN).clear();
        floorsHasCalled.get(Direction.UP).clear();
        floorsToGo.clear();
    }

    public boolean containsFloorForCurrentDirection(int currentFloor, Direction currentDirection, int peopleInElevator, int cabinSize) {
        if (peopleInElevator < cabinSize) {
            for (int floor : floorsHasCalled.get(currentDirection).keySet()) {
                if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
            }
            for (int floor : floorsHasCalled.get(currentDirection.getOtherDirection()).keySet()) {
                if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
            }
        }
        for (int floor : floorsToGo.keySet()) {
            if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
        }
        return false;
    }

    private boolean isFloorGoodForCurrentDirection(int floor, int currentFloor, Direction currentDirection) {
        return currentDirection == Direction.UP
                && floor > currentFloor
                || currentDirection == Direction.DOWN
                && floor < currentFloor;
    }

    public void addFloorForDirection(int floor, Direction direction) {
        floorsHasCalled.get(direction).put(floor, getNbMaxWait() * 2);
    }

    public void addFloorToGo(int floor, int currentFloor) {
        int diff = Math.abs(currentFloor - floor);
        Integer lastWaitTime = lastWaitTimeBeforeOpen.containsKey(currentFloor) ? lastWaitTimeBeforeOpen.get(currentFloor) : 0;
        int wait = getNbMaxWait() + diff - ((getNbMaxWait() * 2 - lastWaitTime) / 2);
        if (!floorsToGo.containsKey(floor)
                || floorsToGo.get(floor) < wait) {
            floorsToGo.put(floor, wait);
        }
    }

    public boolean mustOpenFloorForThisDirection(int currentFloor, Direction currentDirection,
                                                 int peopleInElevator, int cabinSize) {
        return (floorsToGo.containsKey(currentFloor)
                || (floorsHasCalled.get(currentDirection).containsKey(currentFloor)
                    && peopleInElevator < cabinSize));
    }

    public void willOpenDoorsOnFloor(int floor) {
        Integer waitTimeDown = floorsHasCalled.get(Direction.DOWN).remove(floor);
        Integer waitTimeUp = floorsHasCalled.get(Direction.UP).remove(floor);
        int waitTime = getNbMaxWait() * 2;
        if (waitTimeDown != null && waitTimeUp != null) {
            waitTime = Math.max(waitTimeDown, waitTimeUp);
        } else if (waitTimeDown != null) {
            waitTime = waitTimeDown;
        } else if (waitTimeUp != null) {
            waitTime = waitTimeUp;
        }
        lastWaitTimeBeforeOpen.put(floor, waitTime);
    }

    public boolean isEmpty() {
        return floorsHasCalled.get(Direction.DOWN).isEmpty() && floorsHasCalled.get(Direction.UP).isEmpty()
                && floorsToGo.isEmpty();
    }

    @Override
    public void nextCommandCalled(int currentFloor) {
        for (Direction direction : Direction.values()) {
            Set<Integer> floorsToRemove = new HashSet<>();
            for (Map.Entry<Integer, Integer> floorWithCounter : floorsHasCalled.get(direction).entrySet()) {
                floorWithCounter.setValue(floorWithCounter.getValue() - 1);
                if (floorWithCounter.getValue() <= Math.abs(currentFloor - floorWithCounter.getKey())) {
                    floorsToRemove.add(floorWithCounter.getKey());
                }
            }
            for (int floor : floorsToRemove) {
                floorsHasCalled.get(direction).remove(floor);
            }
        }
        Set<Integer> floorsToRemove = new HashSet<>();
        for (Map.Entry<Integer, Integer> floorWithCounter : floorsToGo.entrySet()) {
            floorWithCounter.setValue(floorWithCounter.getValue() - 1);
            if (floorWithCounter.getValue() <= Math.abs(currentFloor - floorWithCounter.getKey())) {
                floorsToRemove.add(floorWithCounter.getKey());
            }
        }
        for (int floor : floorsToRemove) {
            floorsToGo.remove(floor);
        }

    }


}
