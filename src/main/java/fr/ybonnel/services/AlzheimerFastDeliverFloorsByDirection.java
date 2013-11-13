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

import java.util.Map;

public class AlzheimerFastDeliverFloorsByDirection extends AlzheimerFloorsByDirection {

    @Override
    public boolean containsFloorForCurrentDirection(int currentFloor, Direction currentDirection, int peopleInElevator, int cabinSize) {
        if (floorsToGo.isEmpty() && peopleInElevator < cabinSize) {
            int floorToGo = getFloorWithMaxWait(currentFloor);
            if (floorToGo != -1) {
                return isFloorGoodForCurrentDirection(floorToGo, currentFloor, currentDirection);
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

    private int getFloorWithMaxWait(int currentFloor) {
        int maxWait = 0;
        int floorToGo = -1;
        for (Direction direction : Direction.values()) {
            for (Map.Entry<Integer, Integer> floor : floorsHasCalled.get(direction).entrySet()) {
                if (Math.abs(floor.getValue() - currentFloor) > maxWait) {
                    maxWait = Math.abs(floor.getValue() - currentFloor);
                    floorToGo = floor.getKey();
                }
            }
        }
        return floorToGo;
    }

    @Override
    public boolean mustOpenFloorForThisDirection(int currentFloor, Direction currentDirection, int peopleInElevator, int cabinSize) {
        return (floorsToGo.containsKey(currentFloor)
                || (floorsToGo.isEmpty() &&
            getFloorWithMaxWait(currentFloor) == currentFloor)
                || (
                (floorsToGo.isEmpty() && ((floorsHasCalled.get(Direction.DOWN).isEmpty()
                        && floorsHasCalled.get(Direction.UP).isEmpty())
                        || peopleInElevator >= cabinSize))
                        && oldFloorsToGo.contains(currentFloor)));
    }
}
