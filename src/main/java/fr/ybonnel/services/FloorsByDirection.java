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

public class FloorsByDirection  implements IFloorsByDirection {

    private Map<Direction, HashSet<Integer>> floorsToGo = new HashMap<Direction, HashSet<Integer>>(){{
        put(Direction.DOWN, new HashSet<Integer>());
        put(Direction.UP, new HashSet<Integer>());
    }};

    @Override
    public String toString() {
        return floorsToGo.toString();
    }

    public void clear() {
        floorsToGo.get(Direction.DOWN).clear();
        floorsToGo.get(Direction.UP).clear();
    }

    public boolean containsFloorForCurrentDirection(int currentFloor, Direction currentDirection) {
        for (int floor : floorsToGo.get(currentDirection)) {
            if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
        }
        for (int floor : floorsToGo.get(currentDirection.getOtherDirection())) {
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
        floorsToGo.get(direction).add(floor);
    }

    public boolean mustOpenFloorForThisDirection(int currentFloor, Direction currentDirection) {
        return floorsToGo.get(currentDirection).contains(currentFloor);
    }

    public void willOpenDoorsOnFloor(int floor) {
        floorsToGo.get(Direction.DOWN).remove(floor);
        floorsToGo.get(Direction.UP).remove(floor);
    }

    public boolean isEmpty() {
        return floorsToGo.get(Direction.DOWN).isEmpty() && floorsToGo.get(Direction.UP).isEmpty();
    }

    @Override
    public void nextCommandCalled(int currentFloor) {
    }

    public void addFloorToGo(int floor, int currentFloor) {
        floorsToGo.get(Direction.UP).add(floor);
        floorsToGo.get(Direction.DOWN).add(floor);
    }

    @Override
    public void setLowerFloor(Integer lowerFloor) {
    }

    @Override
    public void setHigherFloor(Integer higherFloor) {
    }
}
