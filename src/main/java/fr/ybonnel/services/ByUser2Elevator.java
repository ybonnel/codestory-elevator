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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ByUser2Elevator extends CleverElevator {

    private int bestFloorToWait;

    public void setBestFloorToWait(int bestFloorToWait) {
        this.bestFloorToWait = bestFloorToWait;
    }



    public ByUser2Elevator() {
    }

    private int currentTick = -1;
    private int currentScore;

    protected Direction currentDirection;

    private Map<Integer, LinkedList<User>> waitingUsers = new HashMap<>();
    private Map<Integer, LinkedList<User>> toGoUsers = new HashMap<>();
    private LinkedList<User> usersJustEntered = new LinkedList<>();


    @Override
    protected Command getNextCommand() {
        currentTick++;

        if (hasFloorsToGo()) {
            if (isOpen()) {
                if (toGoUsers.containsKey(currentFloor)) {
                    toGoUsers.remove(currentFloor);
                }
                if (waitingUsers.containsKey(currentFloor)) {
                    Iterator<User> it = waitingUsers.get(currentFloor).iterator();
                    while (it.hasNext()) {
                        if (it.next().getDirectionCalled() == currentDirection) {
                            it.remove();
                        }
                    }
                    if (waitingUsers.get(currentFloor).isEmpty()) {
                        waitingUsers.remove(currentFloor);
                    }
                }

                return close();
            } else {
                if (hasUsersWithScores()) {
                    int scoreIfOpen = estimateScore(currentFloor, currentDirection, true);
                    int scoreIfNoOpen = estimateScore(currentFloor, currentDirection, false);

                    if (scoreIfOpen > scoreIfNoOpen) {
                        return openIfCan();
                    }

                    if (scoreIfNoOpen == 0 && !thereIsUsersWaitingForCurrentDirectionWithScore()) {
                        currentDirection = currentDirection.getOtherDirection();
                        scoreIfOpen = estimateScore(currentFloor, currentDirection, true);
                        scoreIfNoOpen = estimateScore(currentFloor, currentDirection, false);

                        if (scoreIfOpen > scoreIfNoOpen) {
                            return openIfCan();
                        }
                    }

                    currentFloor += currentDirection.incForCurrentFloor;
                    return currentDirection.commandToGo;
                } else {
                    if (mustOpenWithNoScore()) {
                        return openIfCan();
                    }
                    if (!thereIsUsersForCurrentDirection()) {
                        currentDirection = currentDirection.getOtherDirection();
                        if (mustOpenWithNoScore()) {
                            return openIfCan();
                        }
                    }

                    currentFloor += currentDirection.incForCurrentFloor;
                    return currentDirection.commandToGo;
                }
            }
        } else {
            return goToBestFloorToWait();
        }
    }

    private boolean thereIsUsersForCurrentDirection() {
        for (int floor = currentFloor + currentDirection.incForCurrentFloor; currentDirection == Direction.UP && floor <= higherFloor || currentDirection == Direction.DOWN && floor >= lowerFloor; floor += currentDirection.incForCurrentFloor) {
            if (waitingUsers.containsKey(floor) || toGoUsers.containsKey(floor)) {
                return true;
            }
        }
        return false;
    }

    private boolean thereIsUsersWaitingForCurrentDirectionWithScore() {
        for (int floor = currentFloor + currentDirection.incForCurrentFloor; currentDirection == Direction.UP && floor <= higherFloor || currentDirection == Direction.DOWN && floor >= lowerFloor; floor += currentDirection.incForCurrentFloor) {
            if (waitingUsers.containsKey(floor)) {
                for (User user : waitingUsers.get(floor)) {
                    if (user.esperateScore(currentTick, currentFloor) > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean mustOpenWithNoScore() {
        if (toGoUsers.containsKey(currentFloor)) {
            return true;
        }
        if (waitingUsers.containsKey(currentFloor)) {
            for (User user : waitingUsers.get(currentFloor)) {
                if (user.getDirectionCalled() == currentDirection) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addUserWaiting(User user) {
        if (!waitingUsers.containsKey(user.getStartFloor())) {
            waitingUsers.put(user.getStartFloor(), new LinkedList<User>());
        }
        waitingUsers.get(user.getStartFloor()).add(user);
    }

    public String state() {
        StringBuilder builder = new StringBuilder();
        builder.append("currentFloor(").append(currentFloor).append("),");
        builder.append("currentDirection(").append(currentDirection).append("),");
        Map<Integer, List<String>> toGoUsersString = new HashMap<>();
        for (Map.Entry<Integer, LinkedList<User>> entry : toGoUsers.entrySet()) {
            toGoUsersString.put(entry.getKey(), User.userToState(entry.getValue(), currentTick, entry.getKey()));
        }
        builder.append("toGoUsers(").append(toGoUsersString).append("),");
        Map<Integer, List<String>> waitingsUsersString = new HashMap<>();
        for (Map.Entry<Integer, LinkedList<User>> entry : waitingUsers.entrySet()) {
            waitingsUsersString.put(entry.getKey(), User.userToState(entry.getValue(), currentTick, entry.getKey()));
        }
        builder.append("waitingUsers(").append(waitingsUsersString).append("),");
        builder.append("peopleInsideElevator(").append(peopleInsideElevator).append(")");
        return builder.toString();
    }

    public Map<Integer, LinkedList<User>> getWaitingUsers() {
        return waitingUsers;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    public int getResetCount() {
        return resetCount;
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
            if (waitingUsers.containsKey(floor)) {
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
            score += scoreOfUser;
        }
        return score;
    }

    private boolean hasFloorsToGo() {
        return !waitingUsers.isEmpty() || !toGoUsers.isEmpty();
    }


    public boolean hasUsersWithScores() {
        for (Map.Entry<Integer, LinkedList<User>> entries : waitingUsers.entrySet()) {
            for (User user : entries.getValue()) {
                if (user.esperateScore(currentTick, currentFloor) > 0) {
                    return true;
                }
            }
        }
        for (Map.Entry<Integer, LinkedList<User>> entries : toGoUsers.entrySet()) {
            for (User user : entries.getValue()) {
                if (user.esperateScore(currentTick, currentFloor) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public int howManyFloorsWithScores() {
        Set<Integer> floors = new HashSet<>();
        for (Map.Entry<Integer, LinkedList<User>> entries : waitingUsers.entrySet()) {
            for (User user : entries.getValue()) {
                if (user.esperateScore(currentTick, currentFloor) > 0) {
                    floors.add(entries.getKey());
                }
            }
        }
        for (Map.Entry<Integer, LinkedList<User>> entries : toGoUsers.entrySet()) {
            for (User user : entries.getValue()) {
                if (user.esperateScore(currentTick, currentFloor) > 0) {
                    floors.add(entries.getKey());
                }
            }
        }
        return floors.size();
    }

    @Override
    protected void addGo(int floorToGo) {
        User user = usersJustEntered.isEmpty()
                ? new User(currentFloor, currentTick - 50,
                currentFloor < floorToGo ? Direction.UP : Direction.DOWN)
                : usersJustEntered.removeFirst();
        user.go(floorToGo, currentTick);
        if (!toGoUsers.containsKey(floorToGo)) {
            toGoUsers.put(floorToGo, new LinkedList<User>());
        }
        toGoUsers.get(floorToGo).addLast(user);
    }

    @Override
    public int getBestFloorToWait() {
        return bestFloorToWait;
    }


    @Override
    Command getOpenForCurrentDirection() {
        return currentDirection == Direction.UP ? Command.OPEN_UP : Command.OPEN_DOWN;
    }

    @Override
    public void userHasEntered() {
        super.userHasEntered();

        boolean userFound = false;
        if (waitingUsers.containsKey(currentFloor)) {
            Iterator<User> itUsers = waitingUsers.get(currentFloor).iterator();

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
