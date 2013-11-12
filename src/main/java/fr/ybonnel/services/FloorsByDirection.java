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

public class FloorsByDirection  implements IFloorsByDirection {

    private Set<Integer> floorsToGo = new HashSet<>();

    private Map<Direction, HashSet<Integer>> floorsHasCalled = new HashMap<Direction, HashSet<Integer>>(){{
        put(Direction.DOWN, new HashSet<Integer>());
        put(Direction.UP, new HashSet<Integer>());
    }};

    @Override
    public String toString() {
        return floorsToGo.toString();
    }

    public void clear() {
        floorsToGo.clear();
        floorsHasCalled.get(Direction.DOWN).clear();
        floorsHasCalled.get(Direction.UP).clear();
    }

    public boolean containsFloorForCurrentDirection(int currentFloor, Direction currentDirection, int peopleInElevator, int cabinSize) {
        if (peopleInElevator < cabinSize) {
            for (int floor : floorsHasCalled.get(currentDirection)) {
                if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
            }
            for (int floor : floorsHasCalled.get(currentDirection.getOtherDirection())) {
                if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
            }
        }
        for (int floor : floorsToGo) {
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
        floorsHasCalled.get(direction).add(floor);
    }

    public boolean mustOpenFloorForThisDirection(int currentFloor, Direction currentDirection, int peopleInElevator, int cabinSize) {
        return (peopleInElevator < cabinSize && floorsHasCalled.get(currentDirection).contains(currentFloor))
                || floorsToGo.contains(currentFloor);
    }

    public void willOpenDoorsOnFloor(int floor) {
        floorsHasCalled.get(Direction.DOWN).remove(floor);
        floorsHasCalled.get(Direction.UP).remove(floor);
        floorsToGo.remove(floor);
    }

    public boolean isEmpty() {
        return floorsHasCalled.get(Direction.DOWN).isEmpty() && floorsHasCalled.get(Direction.UP).isEmpty()
                && floorsToGo.isEmpty();
    }

    @Override
    public void nextCommandCalled(int currentFloor) {
    }

    public void addFloorToGo(int floor, int currentFloor) {
        floorsToGo.add(floor);
    }

    @Override
    public void setLowerFloor(Integer lowerFloor) {
    }

    @Override
    public void setHigherFloor(Integer higherFloor) {
    }
}
