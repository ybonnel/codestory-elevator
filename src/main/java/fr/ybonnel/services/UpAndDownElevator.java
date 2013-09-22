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

public class UpAndDownElevator extends CleverElevator {

    Direction currentDirection;

    private boolean floorsToGo[] = new boolean[6];

    public UpAndDownElevator() {
        reset(null);
    }

    public boolean containsFloorForCurrentDirection() {

        for (int floor = (currentFloor + currentDirection.incForCurrentFloor);
             floor >= 0 && floor <= 5;
             floor += currentDirection.incForCurrentFloor) {
            if (floorsToGo[floor]) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFloorsToGo() {
        for (boolean floor : floorsToGo) {
            if (floor) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Command nextCommand() {
        if (hasFloorsToGo()) {
            if (isOpen()) {
                return close();
            } else {
                if (floorsToGo[currentFloor]) {
                    floorsToGo[currentFloor] = false;
                    return openIfCan();
                }
                if (!containsFloorForCurrentDirection()) {
                    currentDirection = currentDirection.getOtherDirection();
                }

                currentFloor += currentDirection.incForCurrentFloor;
                return currentDirection.commandToGo;
            }
        } else {
            return openIfCan();
        }
    }

    @Override
    public void call(int floor, String to) {
        floorsToGo[floor] = true;
    }

    @Override
    public void go(int floorToGo) {
        floorsToGo[floorToGo] = true;
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
        for (int floor = 0; floor < floorsToGo.length;floor++) {
            floorsToGo[floor] = false;
        }
    }
}
