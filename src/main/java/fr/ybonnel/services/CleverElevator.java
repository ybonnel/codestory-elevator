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

import fr.ybonnel.services.model.Command;
import fr.ybonnel.services.model.Direction;
import fr.ybonnel.services.model.State;

public abstract class CleverElevator {

    protected int currentFloor = 0;
    State currentState = State.CLOSE;

    protected int lowerFloor;
    protected int higherFloor;

    protected int peopleInsideElevator = 0;
    protected int cabinSize = 9999;
    protected boolean peopleActivity = false;

    public final Command nextCommand() {
        Command command = getNextCommand();
        peopleActivity = false;
        return command;
    }

    protected abstract Command getNextCommand();

    abstract Command getOpenForCurrentDirection();

    protected Command openIfCan() {
        if (isClose()) {
            currentState = State.OPEN;
            return getOpenForCurrentDirection();
        } else {
            return Command.NOTHING;
        }
    }

    public void go(int floorToGo) {
        if (floorToGo >= lowerFloor && floorToGo <= higherFloor) {
            addGo(floorToGo);
        }
    }

    protected abstract void addGo(int floorToGo);

    abstract int getBestFloorToWait();

    protected Command goToBestFloorToWait() {
        if (isOpen()) {
            return close();
        }
        int bestFloorToWait = getBestFloorToWait();
        if (currentFloor < bestFloorToWait) {
            currentFloor++;
            return Command.UP;
        }
        if (currentFloor > bestFloorToWait) {
            currentFloor--;
            return Command.DOWN;
        }
        return Command.NOTHING;
    }

    public void reset(String cause, Integer lowerFloor, Integer higherFloor, Integer cabinSize, Direction currentDirection) {
        currentFloor = 0;
        currentState = State.CLOSE;
        this.lowerFloor = lowerFloor;
        this.higherFloor = higherFloor;
        peopleInsideElevator = 0;
        this.cabinSize = cabinSize;
    }

    protected boolean isOpen() {
        return currentState == State.OPEN;
    }

    protected boolean isClose() {
        return currentState == State.CLOSE;
    }

    protected Command close() {
        currentState = State.CLOSE;
        return Command.CLOSE;
    }

    public void userHasEntered() {
        peopleInsideElevator++;
        peopleActivity = true;
    }

    public void userHasExited() {
        peopleInsideElevator--;
        peopleActivity = true;
    }
}
