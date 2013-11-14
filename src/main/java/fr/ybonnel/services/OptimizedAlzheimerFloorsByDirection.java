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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OptimizedAlzheimerFloorsByDirection implements IFloorsByDirection {

    private int getNbMaxWait() {
        return 19;
    }

    public void setLowerFloor(Integer lowerFloor) {
    }

    public void setHigherFloor(Integer higherFloor) {
    }

    protected Map<Direction, Map<Integer, List<Integer>>> floorsHasCalled = new HashMap<Direction, Map<Integer, List<Integer>>>() {{
        put(Direction.DOWN, new HashMap<Integer, List<Integer>>());
        put(Direction.UP, new HashMap<Integer, List<Integer>>());
    }};

    protected Map<Direction, Map<Integer, List<Integer>>> oldFloorsHasCalled = new HashMap<Direction, Map<Integer, List<Integer>>>() {{
        put(Direction.DOWN, new HashMap<Integer, List<Integer>>());
        put(Direction.UP, new HashMap<Integer, List<Integer>>());
    }};

    protected Map<Integer, Integer> floorsToGo = new HashMap<>();

    protected Set<Integer> oldFloorsToGo = new HashSet<>();

    private Map<Integer, Map<Direction, List<Integer>>> lastWaitTimeBeforeOpen = new HashMap<>();

    @Override
    public String toString() {
        return "FloorsToGo : " + floorsToGo.toString() + ", FloorsHasCalled : " + floorsHasCalled + ", OldFloorsToGo : " + oldFloorsToGo;
    }

    public void clear() {
        floorsHasCalled.get(Direction.DOWN).clear();
        floorsHasCalled.get(Direction.UP).clear();
        oldFloorsHasCalled.get(Direction.DOWN).clear();
        oldFloorsHasCalled.get(Direction.UP).clear();
        floorsToGo.clear();
        oldFloorsToGo.clear();
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
        if (floorsToGo.isEmpty() && ((floorsHasCalled.get(Direction.DOWN).isEmpty()
                && floorsHasCalled.get(Direction.UP).isEmpty())
                || peopleInElevator >= cabinSize)) {
            for (int floor : oldFloorsToGo) {
                if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
            }
        }
        return false;
    }

    protected boolean isFloorGoodForCurrentDirection(int floor, int currentFloor, Direction currentDirection) {
        return currentDirection == Direction.UP
                && floor > currentFloor
                || currentDirection == Direction.DOWN
                && floor < currentFloor;
    }

    public void addFloorForDirection(int floor, Direction direction) {
        if (!floorsHasCalled.get(direction).containsKey(floor)) {
            floorsHasCalled.get(direction).put(floor, new ArrayList<Integer>());
        }
        floorsHasCalled.get(direction).get(floor).add(getNbMaxWait() * 2);
    }

    public void addFloorToGo(int floor, int currentFloor) {
        int diff = Math.abs(currentFloor - floor);
        int waitTimeBeforeGo = 0;
        Direction direction = floor > currentFloor ? Direction.UP : Direction.DOWN;

        if (lastWaitTimeBeforeOpen.containsKey(currentFloor)
                && !lastWaitTimeBeforeOpen.get(currentFloor).isEmpty()
                && !lastWaitTimeBeforeOpen.get(currentFloor).get(direction).isEmpty()) {
            waitTimeBeforeGo = lastWaitTimeBeforeOpen.get(currentFloor).get(direction).remove(0);
        }
        int wait = getNbMaxWait() + diff - ((getNbMaxWait() * 2 - waitTimeBeforeGo) / 2);
        if (!floorsToGo.containsKey(floor)
                || floorsToGo.get(floor) < wait) {
            floorsToGo.put(floor, wait);
        }
    }

    public boolean mustOpenFloorForThisDirection(int currentFloor, Direction currentDirection,
                                                 int peopleInElevator, int cabinSize) {
        return (floorsToGo.containsKey(currentFloor)
                || (floorsHasCalled.get(currentDirection).containsKey(currentFloor)
                && peopleInElevator < cabinSize))
                || (
                (floorsToGo.isEmpty() && ((floorsHasCalled.get(Direction.DOWN).isEmpty()
                        && floorsHasCalled.get(Direction.UP).isEmpty())
                        || peopleInElevator >= cabinSize))
                        && oldFloorsToGo.contains(currentFloor));
    }

    public void willOpenDoorsOnFloor(int floor) {
        if (!lastWaitTimeBeforeOpen.containsKey(floor)) {
            lastWaitTimeBeforeOpen.put(floor, new HashMap<Direction, List<Integer>>());
        }
        for (Direction direction : Direction.values()) {
            if (!lastWaitTimeBeforeOpen.get(floor).containsKey(direction)) {
                lastWaitTimeBeforeOpen.get(floor).put(direction, new ArrayList<Integer>());
            }
            if (oldFloorsHasCalled.get(direction).containsKey(floor)) {
                for (int waitTime : oldFloorsHasCalled.get(direction).get(floor)) {
                    lastWaitTimeBeforeOpen.get(floor).get(direction).add(waitTime);
                }
                oldFloorsHasCalled.get(direction).get(floor).clear();
            }
            if (floorsHasCalled.get(direction).containsKey(floor)) {
                for (int waitTime : floorsHasCalled.get(direction).get(floor)) {
                    lastWaitTimeBeforeOpen.get(floor).get(direction).add(waitTime);
                }
                floorsHasCalled.get(direction).get(floor).clear();
            }
        }
        floorsToGo.remove(floor);
        oldFloorsToGo.remove(floor);
    }

    public boolean isEmpty() {
        return floorsHasCalled.get(Direction.DOWN).isEmpty() && floorsHasCalled.get(Direction.UP).isEmpty()
                && floorsToGo.isEmpty()
                && oldFloorsToGo.isEmpty();
    }

    @Override
    public void nextCommandCalled(int currentFloor) {
        for (Direction direction : Direction.values()) {
            for (Map.Entry<Integer, List<Integer>> floorWithCounter : oldFloorsHasCalled.get(direction).entrySet()) {
                List<Integer> newCounters = new ArrayList<>();
                for (int counter : floorWithCounter.getValue()) {
                    newCounters.add(counter - 1);
                }
                floorWithCounter.setValue(newCounters);
            }


            for (Map.Entry<Integer, List<Integer>> floorWithCounter : floorsHasCalled.get(direction).entrySet()) {
                List<Integer> newCounters = new ArrayList<>();
                for (int counter : floorWithCounter.getValue()) {
                    if ((counter - 1) <= Math.abs(currentFloor - floorWithCounter.getKey())) {
                        if (!oldFloorsHasCalled.get(direction).containsKey(floorWithCounter.getKey())) {
                            oldFloorsHasCalled.get(direction).put(floorWithCounter.getKey(), new ArrayList<Integer>());
                        }
                        oldFloorsHasCalled.get(direction).get(floorWithCounter.getKey()).add(counter - 1);
                    } else {
                        newCounters.add(counter - 1);
                    }
                }
                floorWithCounter.setValue(newCounters);
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
            oldFloorsToGo.add(floor);
        }

    }


}
