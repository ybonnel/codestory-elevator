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
import fr.ybonnel.services.model.Commands;
import fr.ybonnel.services.model.Direction;
import fr.ybonnel.services.model.User;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ByUserElevators implements Elevators {

    private Map<Integer, LinkedList<User>> waitingUsers = new HashMap<>();
    private List<ByUserElevator> elevators = new ArrayList<>();

    private List<Integer> peopleByTick = new ArrayList<>();
    private final boolean log;
    private int maxWaitingsMean = 10;
    private int lowerFloor;
    private int higherFlor;

    public int getMaxWaitingsMean() {
        return maxWaitingsMean;
    }

    public List<Integer> getPeopleByTick() {
        return peopleByTick;
    }

    public List<ByUserElevator> getElevators() {
        return elevators;
    }

    private static final Logger logger = LoggerFactory.getLogger(ByUserElevators.class);

    private int currentTick = -1;
    private boolean mustReset = true;
    private int cabinSize = 30;

    public ByUserElevators() {
        log = true;
    }

    public ByUserElevators(boolean log, int maxWaiting) {
        this.log = log;
        this.maxWaitingsMean = maxWaiting;
    }

    @Override
    public void logState() {
        if (log) {
            Map<Integer, List<String>> waitingUsersString = new HashMap<>();
            for (Map.Entry<Integer, LinkedList<User>> entry : waitingUsers.entrySet()) {
                waitingUsersString.put(entry.getKey(), User.userToState(entry.getValue(), currentTick, entry.getKey()));
            }
            logger.info("Waiting users : {}", waitingUsersString);
            int index = 0;
            for (ByUserElevator elevator : elevators) {
                logger.info("Elevator {} : {}", index++, elevator.state());
            }
        }
    }

    @Override
    public Commands nextCommands() {
        if (mustReset) {
            return new Commands(Arrays.asList(Command.FORCERESET));
        }
        currentTick++;
        peopleByTick.add(0);
        logState();

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int floor = lowerFloor; floor <= higherFlor; floor++) {
            stats.addValue(waitingUsers.containsKey(floor) ? waitingUsers.get(floor).size() : 0);
        }
        if (stats.getMean() > maxWaitingsMean) {
            return new Commands(Arrays.asList(Command.FORCERESET));
        }

        boolean mustChangeDirectionToBetterScore = true;
        boolean hasScore = false;
        boolean canDoScore = false;

        for (ByUserElevator elevator : elevators) {
            elevator.setHasScore(true);
            if (elevator.hasUsersWithScores()) {
                hasScore = true;
            }
            if (elevator.estimateScore(elevator.getCurrentFloor(), elevator.currentDirection, true) > 0
                    || elevator.estimateScore(elevator.getCurrentFloor(), elevator.currentDirection, false) > 0
                    || elevator.estimateScore(elevator.getCurrentFloor(), elevator.currentDirection.getOtherDirection(), true) > 0
                    || elevator.estimateScore(elevator.getCurrentFloor(), elevator.currentDirection.getOtherDirection(), false) > 0) {
                canDoScore = true;
            }
        }
        if (!canDoScore) {
            hasScore = false;
        }


        for (ByUserElevator elevator : elevators) {
            elevator.setHasScore(hasScore);
            if (elevator.getPeopleInsideElevator() >= cabinSize) {
                elevator.setHasScore(false);
            }
            if (elevator.hasUsersForCurrentDirection()) {
                mustChangeDirectionToBetterScore = false;
            }
        }


        if (mustChangeDirectionToBetterScore && hasScore) {
            if (log) {
                logger.info("Change direction : hasScore({}), mustChangeDirectionToBetterScore({})", hasScore, mustChangeDirectionToBetterScore);
            }
            int midCabin = elevators.size() / 2;
            int floorsByCabin = (higherFlor - lowerFloor) / midCabin;
            for (ByUserElevator elevator : elevators) {
                if (elevator.currentDirection == Direction.UP) {
                    if (elevator.getCurrentMaxFloor() == higherFlor) {
                        elevator.currentDirection = Direction.DOWN;
                        elevator.setCurrentMaxFloor(higherFlor);
                        elevator.setCurrentMinFloor(lowerFloor + (floorsByCabin * (midCabin - 1)));
                    } else {
                        elevator.setCurrentMaxFloor(elevator.getCurrentMaxFloor() + floorsByCabin);
                        elevator.setCurrentMinFloor(elevator.getCurrentMinFloor() + floorsByCabin);
                        if (Math.abs(higherFlor - elevator.getCurrentMaxFloor()) < floorsByCabin) {
                            elevator.setCurrentMaxFloor(higherFlor);
                        }
                    }
                } else {
                    if (elevator.getCurrentMinFloor() == lowerFloor) {
                        elevator.currentDirection = Direction.UP;
                        elevator.setCurrentMaxFloor(lowerFloor + floorsByCabin);
                        elevator.setCurrentMinFloor(lowerFloor);
                        elevator.setCurrentMaxFloor(elevator.getCurrentMaxFloor() - floorsByCabin);
                        elevator.setCurrentMinFloor(elevator.getCurrentMinFloor() - floorsByCabin);
                        if (Math.abs(lowerFloor - elevator.getCurrentMinFloor()) < floorsByCabin) {
                            elevator.setCurrentMaxFloor(lowerFloor);
                        }
                    }
                }

            }
        }



        List<Command> commands = new ArrayList<>();
        for (ByUserElevator elevator : elevators) {
            commands.add(elevator.nextCommand());
        }


        return new Commands(commands);
    }

    @Override
    public void call(int floor, String to) {
        if (currentTick >= 0 && currentTick < peopleByTick.size()) {
            peopleByTick.set(currentTick, peopleByTick.get(currentTick)+1);
        }
        if (!waitingUsers.containsKey(floor)) {
            waitingUsers.put(floor, new LinkedList<User>());
        }
        waitingUsers.get(floor).addLast(new User(floor, currentTick, Direction.valueOf(to)));
    }

    @Override
    public void go(int cabin, int floorToGo) {
        elevators.get(cabin).go(floorToGo);
    }

    @Override
    public void userHasEntered(int cabin) {
        elevators.get(cabin).userHasEntered();
    }

    @Override
    public void userHasExited(int cabin) {
        elevators.get(cabin).userHasExited();
    }

    @Override
    public void reset(String cause, int lowerFloor, int higherFloor, int cabinSize, int cabinCount) {
        mustReset = false;
        waitingUsers.clear();
        this.lowerFloor = lowerFloor;
        this.higherFlor = higherFloor;
        this.cabinSize = cabinSize;
        int midCabin = cabinCount / 2;
        Direction direction = Direction.DOWN;
        if (cabinCount != elevators.size()) {
            elevators.clear();
            for (int cabinIndex = 0; cabinIndex < cabinCount; cabinIndex++) {
                if (cabinCount == midCabin) {
                    direction = direction.getOtherDirection();
                }
                elevators.add(new ByUserElevator(waitingUsers, direction));
            }
        }

        if (cause.startsWith("all elevators are at floor")) {
            currentTick = -1;
            peopleByTick.clear();
        }

        direction = Direction.DOWN;
        int cabinIndex = 0;
        int cabinIndexForDirection = 0;
        int floorsByCabin = (higherFloor - lowerFloor) / midCabin;
        for (ByUserElevator elevator : elevators) {
            if (cabinIndex == midCabin) {
                direction = direction.getOtherDirection();
                cabinIndexForDirection = 0;
            }
            elevator.reset(cause, lowerFloor, higherFloor, cabinSize, direction);
            elevator.setCurrentMinFloor(lowerFloor + floorsByCabin * cabinIndexForDirection);

            elevator.setCurrentMaxFloor(cabinIndexForDirection == (midCabin - 1)
                    ? higherFloor
                    : lowerFloor + floorsByCabin * (cabinIndexForDirection + 1));

            cabinIndexForDirection++;
            cabinIndex++;
        }
    }

    public int getCurrentTick() {
        return currentTick;
    }
}