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

    private final static int MAX_WAIT = 20;
    private final static int MAX_WAIT_FOR_GO = 10;

    private Map<Direction, Map<Integer, Integer>> floorsToGo = new HashMap<Direction, Map<Integer, Integer>>(){{
        put(Direction.DOWN, new HashMap<Integer, Integer>());
        put(Direction.UP, new HashMap<Integer, Integer>());
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
        for (int floor : floorsToGo.get(currentDirection).keySet()) {
            if (isFloorGoodForCurrentDirection(floor, currentFloor, currentDirection)) return true;
        }
        for (int floor : floorsToGo.get(currentDirection.getOtherDirection()).keySet()) {
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
        floorsToGo.get(direction).put(floor, MAX_WAIT);
    }

    public void addFloorToGo(int floor) {
        floorsToGo.get(Direction.UP).put(floor, MAX_WAIT_FOR_GO);
        floorsToGo.get(Direction.DOWN).put(floor, MAX_WAIT_FOR_GO);
    }

    public boolean mustOpenFloorForThisDirection(int currentFloor, Direction currentDirection) {
        return floorsToGo.get(currentDirection).containsKey(currentFloor);
    }

    public void willOpenDoorsOnFloor(int floor) {
        floorsToGo.get(Direction.DOWN).remove(floor);
        floorsToGo.get(Direction.UP).remove(floor);
    }

    public boolean isEmpty() {
        return floorsToGo.get(Direction.DOWN).isEmpty() && floorsToGo.get(Direction.UP).isEmpty();
    }

    @Override
    public void nextCommandCalled() {
        for (Direction direction : Direction.values()) {
            Set<Integer> floorsToRemove = new HashSet<>();
            for (Map.Entry<Integer, Integer> floorWithCounter : floorsToGo.get(direction).entrySet()) {
                floorWithCounter.setValue(floorWithCounter.getValue()-1);
                if (floorWithCounter.getValue() == 0) {
                    floorsToRemove.add(floorWithCounter.getKey());
                }
            }
            for (int floor : floorsToRemove) {
                floorsToGo.get(direction).remove(floor);
            }
        }
    }


}
