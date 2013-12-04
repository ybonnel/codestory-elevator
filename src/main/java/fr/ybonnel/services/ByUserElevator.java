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
import fr.ybonnel.services.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ByUserElevator extends CleverElevator {

    private static final Logger logger = LoggerFactory.getLogger(ByUserElevator.class);
    private boolean hasScore = true;

    public ByUserElevator(Map<Integer, LinkedList<User>> waitingUsers, Direction currentDirection) {
        this.waitingUsers = waitingUsers;
        this.currentDirection = currentDirection;
    }

    private int currentTick = -1;
    private int currentScore;

    protected Direction currentDirection;

    public String state() {
        StringBuilder builder = new StringBuilder();
        builder.append("currentFloor(").append(currentFloor).append("),");
        builder.append("currentDirection(").append(currentDirection).append("),");
        Map<Integer, List<String>> toGoUsersString = new HashMap<>();
        for (Map.Entry<Integer, LinkedList<User>> entry : toGoUsers.entrySet()) {
            toGoUsersString.put(entry.getKey(), User.userToState(entry.getValue(), currentTick, entry.getKey()));
        }
        builder.append("toGoUsers(").append(toGoUsersString).append("),");
        builder.append("peopleInsideElevator(").append(peopleInsideElevator).append(")");
        return builder.toString();
    }

    private Map<Integer, LinkedList<User>> waitingUsers = new HashMap<>();
    private Map<Integer, LinkedList<User>> toGoUsers = new HashMap<>();

    private LinkedList<User> usersJustEntered = new LinkedList<>();

    public boolean hasUsersWithScores() {
        for (Map.Entry<Integer, LinkedList<User>> entries : waitingUsers.entrySet()) {
            for (User user : entries.getValue()) {
                if (user.esperateScore(currentTick, entries.getKey()) > 0 || !hasScore) {
                    return true;
                }
            }
        }
        for (Map.Entry<Integer, LinkedList<User>> entries : toGoUsers.entrySet()) {
            for (User user : entries.getValue()) {
                if (user.esperateScore(currentTick, entries.getKey()) > 0 || !hasScore) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasUsersForCurrentDirection() {
        for (Map.Entry<Integer, LinkedList<User>> entries : waitingUsers.entrySet()) {
            if (entries.getKey() >= currentMinFloor && entries.getKey() <= currentMaxFloor
                    && currentDirection.floorIsOnDirection(currentFloor, entries.getKey())) {
                for (User user : entries.getValue()) {
                    if ((currentFloor != entries.getKey()
                        || currentDirection == user.getDirectionCalled()) && (user.esperateScore(currentTick, entries.getKey()) > 0 || !hasScore)) {
                        return true;
                    }
                }
            }
        }
        for (Map.Entry<Integer, LinkedList<User>> entries : toGoUsers.entrySet()) {
            if (entries.getKey() >= currentMinFloor && entries.getKey() <= currentMaxFloor
                    && currentDirection.floorIsOnDirection(currentFloor, entries.getKey())) {
                for (User user : entries.getValue()) {
                    if (user.esperateScore(currentTick, entries.getKey()) > 0
                            || !hasScore) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected Command getNextCommand() {
        currentTick++;
        if (hasFloorsToGo()) {
            if (isOpen()) {
                if (!peopleActivity
                        && peopleInsideElevator < cabinSize
                        && waitingUsers.containsKey(currentFloor)) {
                    logger.warn("Strange state : CLOSE the door but no activity of people");
                    if (currentTick < 100) {
                        // Premier ticks, sans doute un problème de synchro au démarrage
                        return Command.FORCERESET;
                    }
                    // Bizare, on supprime les users qui devait bouger.
                    waitingUsers.remove(currentFloor);
                    toGoUsers.remove(currentFloor);
                }
                return close();
            } else {
                int scoreIfOpen = estimateScore(currentFloor, currentDirection, true);
                int scoreIfNoOpen = estimateScore(currentFloor, currentDirection, false);

                if (scoreIfOpen > scoreIfNoOpen) {
                    return openIfCan();
                }

                if (scoreIfNoOpen == 0) {
                    return goToBestFloorToWait();
                }

                currentFloor += currentDirection.incForCurrentFloor;
                return currentDirection.commandToGo;
            }
        } else {
            return goToBestFloorToWait();
        }
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getResetCount() {
        return resetCount;
    }

    public void setHasScore(boolean hasScore) {
        this.hasScore = hasScore;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    private static class PeopleInElevator {

        private PeopleInElevator(int nbUsersInElevator) {
            this.nbUsersInElevator = nbUsersInElevator;
        }

        int nbUsersInElevator;
    }

    public int estimateScore(int currentFloor, Direction currentDirection, boolean openOnCurrentFloor) {
        int score = 0;
        PeopleInElevator peopleInElevator = new PeopleInElevator(peopleInsideElevator);

        for (int floor = currentFloor; currentDirection == Direction.UP && floor <= higherFloor
                || currentDirection == Direction.DOWN && floor >= lowerFloor; floor += currentDirection.incForCurrentFloor) {
            if (toGoUsers.containsKey(floor)) {
                score += estimateScoreForOneFloor(currentFloor, currentDirection, openOnCurrentFloor, floor, toGoUsers.get(floor), peopleInElevator);
            }
            if (waitingUsers.containsKey(floor)
                    && floor >= currentMinFloor && floor <= currentMaxFloor) {
                score += estimateScoreForOneFloor(currentFloor, currentDirection, openOnCurrentFloor, floor, waitingUsers.get(floor), peopleInElevator);
            }
        }
        return score;
    }

    private int estimateScoreForOneFloor(int currentFloor, Direction currentDirection, boolean openOnCurrentFloor, int floorOfUser, LinkedList<User> users, PeopleInElevator peopleInElevator) {
        int score = 0;
        if (currentDirection.floorIsOnDirection(currentFloor, floorOfUser)) {
            if (currentFloor != floorOfUser || openOnCurrentFloor) {
                for (User user : users) {
                    score = estimateScoreOfOneUser(currentFloor, currentDirection, openOnCurrentFloor, floorOfUser, peopleInElevator, score, user);
                }
            }
        }
        return score > 0 ? score : 0;
    }

    private int estimateScoreOfOneUser(int currentFloor, Direction currentDirection, boolean openOnCurrentFloor, int floorOfUser, PeopleInElevator peopleInElevator, int score, User user) {
        boolean mustCount = false;
        if (user.getDirectionCalled() == currentDirection && user.getDestinationFloor() == null && peopleInElevator.nbUsersInElevator < cabinSize) {
            mustCount = true;
            peopleInElevator.nbUsersInElevator++;
        }

        if (user.getDestinationFloor() != null) {
            mustCount = true;
            peopleInElevator.nbUsersInElevator--;
        }
        if (mustCount) {
            int scoreOfUser = user.esperateScore(currentTick, currentFloor);
            if (currentFloor != floorOfUser && openOnCurrentFloor) {
                scoreOfUser = scoreOfUser - 2;
            }
            if(!hasScore) {
                score += 999999 - (100 * Math.abs(currentFloor - floorOfUser));
            } else if (scoreOfUser > 0) {
                score += scoreOfUser;
            }
        }
        return score;
    }

    private boolean hasFloorsToGo() {
        return hasUsersWithScores();
    }

    @Override
    protected void addGo(int floorToGo) {
        User user = usersJustEntered.removeFirst();
        user.go(floorToGo, currentTick);
        if (!toGoUsers.containsKey(floorToGo)) {
            toGoUsers.put(floorToGo, new LinkedList<User>());
        }
        toGoUsers.get(floorToGo).addLast(user);
    }

    private int currentMinFloor;
    private int currentMaxFloor;

    public void setCurrentMinFloor(int currentMinFloor) {
        this.currentMinFloor = currentMinFloor;
    }

    public void setCurrentMaxFloor(int currentMaxFloor) {
        this.currentMaxFloor = currentMaxFloor;
    }

    public int getCurrentMinFloor() {
        return currentMinFloor;
    }

    public int getCurrentMaxFloor() {
        return currentMaxFloor;
    }

    @Override
    int getBestFloorToWait() {
        if (!hasFloorsToGo()) {
            return currentDirection == Direction.UP ? currentMaxFloor : currentMinFloor;
        } else {
            if (currentDirection == Direction.UP) {
                int maxWaitingFloorWithScore = Integer.MIN_VALUE;
                for (Map.Entry<Integer, LinkedList<User>> entry : waitingUsers.entrySet()) {
                    for (User user : entry.getValue()) {
                        if (user.esperateScore(currentTick, currentFloor) > 0
                                && entry.getKey() > maxWaitingFloorWithScore
                                && entry.getKey() >= currentMinFloor
                                && entry.getKey() <= currentMaxFloor) {
                            maxWaitingFloorWithScore = entry.getKey();
                        }
                    }
                }
                if (maxWaitingFloorWithScore == Integer.MIN_VALUE) {
                    return currentMaxFloor;
                } else {
                    return maxWaitingFloorWithScore;
                }
            } else {
                int minWaitingFloorWithScore = Integer.MAX_VALUE;
                for (Map.Entry<Integer, LinkedList<User>> entry : waitingUsers.entrySet()) {
                    for (User user : entry.getValue()) {
                        if (user.esperateScore(currentTick, currentFloor) > 0
                                && entry.getKey() < minWaitingFloorWithScore
                                && entry.getKey() >= currentMinFloor
                                && entry.getKey() <= currentMaxFloor) {
                            minWaitingFloorWithScore = entry.getKey();
                        }
                    }
                }
                if (minWaitingFloorWithScore == Integer.MAX_VALUE) {
                    return currentMinFloor;
                } else {
                    return minWaitingFloorWithScore;
                }
            }
        }
    }


    @Override
    Command getOpenForCurrentDirection() {
        return currentDirection == Direction.UP ? Command.OPEN_UP : Command.OPEN_DOWN;
    }

    @Override
    public void userHasEntered() {
        super.userHasEntered();
        Iterator<User> itUsers = waitingUsers.get(currentFloor).iterator();
        boolean userFound = false;
        while (itUsers.hasNext() && !userFound) {
            User user = itUsers.next();
            if (user.getDirectionCalled() == currentDirection) {
                usersJustEntered.addLast(user);
                itUsers.remove();
                userFound = true;
            }
        }
        if (waitingUsers.get(currentFloor).isEmpty()) {
            waitingUsers.remove(currentFloor);
        }
    }

    @Override
    public void userHasExited() {
        super.userHasExited();
        if (!toGoUsers.containsKey(currentFloor)) {
            return;
        }
        int score = toGoUsers.get(currentFloor).removeFirst().esperateScore(currentTick, currentFloor);
        if (toGoUsers.get(currentFloor).isEmpty()) {
            toGoUsers.remove(currentFloor);
        }
        currentScore += score;
    }

    private int resetCount = 1;

    @Override
    public void reset(String cause, Integer lowerFloor, Integer higherFloor, Integer cabinSize, Direction currentDirection) {
        super.reset(cause, lowerFloor, higherFloor, cabinSize, currentDirection);
        if (cause.startsWith("all elevators are at floor")) {
            currentScore = 0;
            resetCount = 1;
            currentTick = -1;
        } else {
            currentScore = currentScore - resetCount;
            resetCount++;
        }
        this.currentDirection = currentDirection;
        toGoUsers.clear();
        usersJustEntered.clear();
    }

    public int getCurrentScore() {
        return currentScore;
    }

}
