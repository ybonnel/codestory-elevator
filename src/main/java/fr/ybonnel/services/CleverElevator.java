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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CleverElevator implements Elevator {

    int currentFloor = 0;
    State currentState = State.CLOSE;
    int currentTick = 0;
    int tickOfLastReset = Integer.MIN_VALUE;
    private DescriptiveStatistics statsOfCall = new DescriptiveStatistics(500);

    private Integer lowerFloor;
    private Integer higherFloor;

    private int peopleInsideElevator = 0;
    private int cabinSize = 9999;

    public int getPeopleInsideElevator() {
        return peopleInsideElevator;
    }

    public int getCabinSize() {
        return cabinSize;
    }

    public int getLowerFloor() {
        if (lowerFloor != null) {
            return lowerFloor;
        }
        return 0;
    }

    public Integer getHigherFloor() {
        if (higherFloor != null) {
            return higherFloor;
        }
        return 19;
    }

    private boolean mustReset = true;

    protected Command lastCommand;

    protected CleverElevator() {
        logger = LoggerFactory.getLogger(getClass());
    }

    private final Logger logger;

    @Override
    public final Command nextCommand() {
        currentTick++;
        if (mustReset || (peopleInsideElevator >= cabinSize && currentTick - tickOfLastReset > 1000)) {
            lastCommand = Command.FORCERESET;
        } else {
            lastCommand = getNextCommand();
        }
        if (lastCommand == Command.CLOSE && !peopleActivity) {
            logger.warn("Strange state : CLOSE the door but no activity of people");
            //lastCommand = Command.FORCERESET;
        }
        peopleActivity = false;
        return lastCommand;
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
    public final void call(int floor, String to) {
        if (floor < getLowerFloor() || floor > getHigherFloor()) {
            return;
        }
        addCallToMap(floor);
        addCall(floor, to);
    }

    protected abstract void addCall(int floor, String to);

    @Override
    public void go(int floorToGo) {
        if (floorToGo >= getLowerFloor() && floorToGo <= getHigherFloor()) {
            addGo(floorToGo);
        }
    }

    protected abstract void addGo(int floorToGo);


    private void addCallToMap(int floor) {
        statsOfCall.addValue(floor);
    }

    private int getBestFloorToWait() {
        if (statsOfCall.getN() > 0) {
            return (int) Math.round(statsOfCall.getMean());
        } else {
            return 0;
        }
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
        tickOfLastReset = currentTick;
        mustReset = false;
        currentFloor = 0;
        currentState = State.CLOSE;
        statsOfCall.clear();
        int mid = (getHigherFloor() + getLowerFloor()) / 2;
        for (int i=0; i < 20; i++) {
            statsOfCall.addValue(mid);
        }
        this.lowerFloor = lowerFloor;
        this.higherFloor = higherFloor;
        peopleInsideElevator = 0;
        this.cabinSize = cabinSize == null ? 9999 : cabinSize;
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

    private boolean peopleActivity = false;

    @Override
    public void userHasEntered() {
        peopleInsideElevator++;
        peopleActivity = true;
    }

    @Override
    public void userHasExited() {
        peopleInsideElevator--;
        peopleActivity = true;
    }
}
