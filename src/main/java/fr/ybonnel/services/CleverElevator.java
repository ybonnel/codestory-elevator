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
import fr.ybonnel.services.model.State;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CleverElevator implements Elevator {

    protected int currentFloor = 0;
    State currentState = State.CLOSE;

    protected int lowerFloor;
    protected int higherFloor;

    protected int peopleInsideElevator = 0;
    protected int cabinSize = 9999;

    @Override
    public final Command nextCommand() {
        return getNextCommand();
    }

    protected abstract Command getNextCommand();

    protected Command openIfCan() {
        if (isClose()) {
            currentState = State.OPEN;
            return Command.OPEN;
        } else {
            return Command.NOTHING;
        }
    }

    @Override
    public void go(int floorToGo) {
        if (floorToGo >= lowerFloor && floorToGo <= higherFloor) {
            addGo(floorToGo);
        }
    }

    protected abstract void addGo(int floorToGo);

    private int getBestFloorToWait() {
        return (higherFloor + lowerFloor) / 2;
    }

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

    @Override
    public void reset(String cause, Integer lowerFloor, Integer higherFloor, Integer cabinSize) {
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

    @Override
    public void userHasEntered() {
        peopleInsideElevator++;
    }

    @Override
    public void userHasExited() {
        peopleInsideElevator--;
    }
}
