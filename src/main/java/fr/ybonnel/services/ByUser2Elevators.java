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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ByUser2Elevators implements Elevators {

    private Map<Integer, Integer> waitingUsersCount = new HashMap<>();
    private List<ByUser2Elevator> elevators = new ArrayList<>();

    private List<Integer> peopleByTick = new ArrayList<>();
    private Map<Integer, Integer> callsByFloor = new TreeMap<>();
    private DescriptiveStatistics statsCalls = new DescriptiveStatistics(500);
    private int maxWaitingsMean = 10;
    private int lowerFloor;
    private int higherFlor;


    private static final Logger logger = LoggerFactory.getLogger(ByUser2Elevators.class);

    private int currentTick = -1;
    private boolean mustReset = true;
    private boolean log = true;

    public ByUser2Elevators() {
    }

    public ByUser2Elevators(boolean log, int maxWaiting) {
        this.log = log;
        this.maxWaitingsMean = maxWaiting;
    }

    @Override
    public void logState() {
        if (log) {
            int index = 0;
            for (ByUser2Elevator elevator : elevators) {
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

        assignWaitingsUsers();


        logState();

        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int floor = lowerFloor; floor <= higherFlor; floor++) {
            stats.addValue(waitingUsersCount.containsKey(floor) ? waitingUsersCount.get(floor) : 0);
        }
        if (stats.getMean() > maxWaitingsMean) {
            return new Commands(Arrays.asList(Command.FORCERESET));
        }

        List<Command> commands = new ArrayList<>();
        for (ByUser2Elevator elevator : elevators) {
            commands.add(elevator.nextCommand());
        }
        if (log) {
            logger.info(commands.toString());
            logState();
        }
        return new Commands(commands);
    }

    private void assignWaitingsUsers() {
        for (ByUser2Elevator elevator : elevators) {
            Set<Integer> waitingFloorsToRemove = new HashSet<>();
            for (Map.Entry<Integer, LinkedList<User>> users : elevator.getWaitingUsers().entrySet()) {

                Iterator<User> itUsers = users.getValue().iterator();
                while (itUsers.hasNext()) {
                    User user = itUsers.next();
                    ByUser2Elevator bestElevator = getBestElevatorForUser(user);
                    if (bestElevator != elevator) {
                        itUsers.remove();
                        bestElevator.addUserWaiting(user);
                    }
                }
                if (users.getValue().isEmpty()) {
                    waitingFloorsToRemove.add(users.getKey());
                }
            }
            for (int floor : waitingFloorsToRemove) {
                elevator.getWaitingUsers().remove(floor);
            }
        }
    }

    @Override
    public void call(int floor, String to) {
        if (currentTick >= 0 && currentTick < peopleByTick.size()) {
            peopleByTick.set(currentTick, peopleByTick.get(currentTick)+1);
        }

        if (!waitingUsersCount.containsKey(floor)) {
            waitingUsersCount.put(floor, 0);
        }
        waitingUsersCount.put(floor, waitingUsersCount.get(floor) + 1);

        if (!callsByFloor.containsKey(floor)) {
            callsByFloor.put(floor, 0);
        }
        callsByFloor.put(floor, callsByFloor.get(floor) + 1);

        statsCalls.addValue(floor);
        setBestFloorToWaitToElevators();

        User user = new User(floor, currentTick, Direction.valueOf(to));

        getBestElevatorForUser(user).addUserWaiting(user);
    }

    private ByUser2Elevator getBestElevatorForUser(User user) {
        // Try to find the nearest elevator.
        ByUser2Elevator nearestElevator = null;
        for (ByUser2Elevator elevator : elevators) {
            if (nearestElevator == null || Math.abs(elevator.currentFloor - user.getStartFloor()) < Math.abs(nearestElevator.currentFloor - user.getStartFloor())) {
                nearestElevator = elevator;
            }
        }

        return nearestElevator;
    }

    @Override
    public void go(int cabin, int floorToGo) {
        elevators.get(cabin).go(floorToGo);
    }

    @Override
    public void userHasEntered(int cabin) {
        int floorOfCabin = elevators.get(cabin).getCurrentFloor();
        if (!waitingUsersCount.containsKey(floorOfCabin)) {
            waitingUsersCount.put(floorOfCabin, 0);
        }
        waitingUsersCount.put(floorOfCabin, waitingUsersCount.get(floorOfCabin)-1);
        elevators.get(cabin).userHasEntered();
    }

    @Override
    public void userHasExited(int cabin) {
        elevators.get(cabin).userHasExited();
    }

    @Override
    public void reset(String cause, int lowerFloor, int higherFloor, int cabinSize, int cabinCount) {
        mustReset = false;
        waitingUsersCount.clear();
        this.lowerFloor = lowerFloor;
        this.higherFlor = higherFloor;
        callsByFloor.clear();
        if (cabinCount != elevators.size()) {
            elevators.clear();
            for (int cabinIndex = 0; cabinIndex < cabinCount; cabinIndex++) {
                elevators.add(new ByUser2Elevator());
            }
        }

        if (cause.startsWith("all elevators are at floor")) {
            currentTick = -1;
            peopleByTick.clear();
            statsCalls.clear();
        }

        if (statsCalls.getN() == 0) {
            for (int floor = lowerFloor; floor <= higherFloor; floor++) {
                statsCalls.addValue(floor);
            }
        }
        Direction currentDirection = Direction.DOWN;
        for (ByUser2Elevator elevator : elevators) {
            elevator.reset(cause, lowerFloor, higherFloor, cabinSize, currentDirection);
            currentDirection = currentDirection.getOtherDirection();
        }
        setBestFloorToWaitToElevators();
    }

    private void setBestFloorToWaitToElevators() {
        int cabinIndex = 0;
        for (ByUser2Elevator elevator : elevators) {
            double percentile = ((double)cabinIndex + 0.5)/ ((double)elevators.size());
            int bestFloorToWait = (int) Math.round(statsCalls.getPercentile(percentile*100));
            elevator.setBestFloorToWait(bestFloorToWait);
            cabinIndex++;
        }
    }

    public List<ByUser2Elevator> getElevators() {
        return elevators;
    }

    public List<Integer> getPeopleByTick() {
        return peopleByTick;
    }

    public Map<Integer, Integer> getCallsByFloor() {
        return callsByFloor;
    }
}