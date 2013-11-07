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

public abstract class CleverElevator implements Elevator {

    int currentFloor = 0;
    State currentState = State.CLOSE;
    private DescriptiveStatistics statsOfCall = new DescriptiveStatistics(500);

    private boolean mustReset = true;

    protected Command lastCommand;

    protected CleverElevator() {
    }

    @Override
    public final Command nextCommand() {
        if (mustReset) {
            lastCommand = Command.CLOSE;
        } else {
            lastCommand = getNextCommand();
        }
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
        addCallToMap(floor);
        addCall(floor, to);
    }

    protected abstract void addCall(int floor, String to);

    private void addCallToMap(int floor) {
        statsOfCall.addValue(floor);
    }

    private int getBestFloorToWait() {
        if (statsOfCall.getN() > 0) {
            return (int) Math.round(statsOfCall.apply(new Median()));
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
    public void reset(String cause) {
        mustReset = false;
        currentFloor = 0;
        currentState = State.CLOSE;
        statsOfCall.clear();
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
}
