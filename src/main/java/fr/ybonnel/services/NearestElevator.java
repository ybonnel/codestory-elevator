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

import java.util.HashSet;

public class NearestElevator extends CleverElevator {


    private HashSet<Integer> floorsToGo = new HashSet<>();

    public NearestElevator() {
        reset(null);
    }

    private int getNearestFloor() {
        int minDiff = 999;
        int minFloor = 0;
        for (int floor : floorsToGo) {
            if (Math.abs(floor - currentFloor) < minDiff) {
                minFloor = floor;
                minDiff = Math.abs(floor - currentFloor);
            }
        }
        return minFloor;
    }

    @Override
    public Command nextCommand() {
        if (floorsToGo.isEmpty()) {
            return openIfCan();
        } else {
            if (isOpen()) {
                return close();
            } else {
                if (floorsToGo.contains(currentFloor)) {
                    floorsToGo.remove(currentFloor);
                    return openIfCan();
                }

                int floorToGo = getNearestFloor();

                Direction direction = floorToGo < currentFloor ? Direction.DOWN : Direction.UP;

                currentFloor += direction.incForCurrentFloor;
                return direction.commandToGo;
            }
        }
    }

    @Override
    public void call(int floor, String to) {
        floorsToGo.add(floor);
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
    }
}
