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
import java.util.LinkedList;
import java.util.Map;

public class ByUserElevator extends CleverElevator {

    public ByUserElevator(Map<Integer, LinkedList<User>> waitingUsers) {
        this.waitingUsers = waitingUsers;
    }

    private int currentTick = -1;
    private int currentScore;
    private Direction currentDirection = Direction.UP;

    private Map<Integer, LinkedList<User>> waitingUsers = new HashMap<>();
    private Map<Integer, LinkedList<User>> toGoUsers = new HashMap<>();

    private LinkedList<User> usersJustEntered = new LinkedList<>();

    @Override
    protected Command getNextCommand() {
        currentTick++;
        if (hasFloorsToGo()) {
            if (isOpen()) {
                return close();
            } else {
                int scoreIfOpen = estimateScore(currentFloor, currentDirection, true);
                int scoreIfNoOpen = estimateScore(currentFloor, currentDirection, false);

                if (scoreIfOpen > scoreIfNoOpen) {
                    return openIfCan();
                }

                if (scoreIfNoOpen == 0 && !mustGoTakeForOtherDirection()) {
                    currentDirection = currentDirection.getOtherDirection();
                    scoreIfOpen = estimateScore(currentFloor, currentDirection, true);
                    scoreIfNoOpen = estimateScore(currentFloor, currentDirection, false);
                    if (scoreIfOpen > scoreIfNoOpen) {
                        return openIfCan();
                    } else if (scoreIfNoOpen == 0) {
                        return goToBestFloorToWait();
                    }
                }

                currentFloor += currentDirection.incForCurrentFloor;
                return currentDirection.commandToGo;
            }
        } else {
            return goToBestFloorToWait();
        }
    }

    private boolean mustGoTakeForOtherDirection() {
        for (Map.Entry<Integer, LinkedList<User>> usersByFloor : waitingUsers.entrySet()) {
            if (currentFloor != usersByFloor.getKey()
                    && currentDirection.floorIsOnDirection(currentFloor, usersByFloor.getKey())) {
                int score = 0;
                for (User user : usersByFloor.getValue()) {
                    if (user.getDirectionCalled() == currentDirection.getOtherDirection()) {
                        score += user.esperateScore(currentTick, currentFloor);
                    }
                }
                if (score > 0) {
                    return true;
                }
            }
        }
        return false;
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

    private int estimateScore(int currentFloor, Direction currentDirection, boolean openOnCurrentFloor) {
        int score = 0;
        PeopleInElevator peopleInElevator = new PeopleInElevator(peopleInsideElevator);
        for (Map.Entry<Integer, LinkedList<User>> usersByFloor : toGoUsers.entrySet()) {
            score += estimateScoreForOneFloor(currentFloor, currentDirection, openOnCurrentFloor, usersByFloor.getKey(), usersByFloor.getValue(), peopleInElevator);
        }
        for (Map.Entry<Integer, LinkedList<User>> usersByFloor : waitingUsers.entrySet()) {
            score += estimateScoreForOneFloor(currentFloor, currentDirection, openOnCurrentFloor, usersByFloor.getKey(), usersByFloor.getValue(), peopleInElevator);
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
            if (scoreOfUser > 0) {
                score += scoreOfUser;
            }
        }
        return score;
    }

    private boolean hasFloorsToGo() {
        return !(waitingUsers.isEmpty() && toGoUsers.isEmpty());
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

    @Override
    public void userHasEntered() {
        super.userHasEntered();
        usersJustEntered.addLast(waitingUsers.get(currentFloor).removeFirst());
        if (waitingUsers.get(currentFloor).isEmpty()) {
            waitingUsers.remove(currentFloor);
        }
    }

    @Override
    public void userHasExited() {
        super.userHasExited();
        int score = toGoUsers.get(currentFloor).removeFirst().esperateScore(currentTick, currentFloor);
        if (toGoUsers.get(currentFloor).isEmpty()) {
            toGoUsers.remove(currentFloor);
        }
        currentScore += score;
    }

    private int resetCount = 1;

    @Override
    public void reset(String cause, Integer lowerFloor, Integer higherFloor, Integer cabinSize) {
        super.reset(cause, lowerFloor, higherFloor, cabinSize);
        if (cause.startsWith("all elevators are at floor")) {
            currentScore = 0;
            resetCount = 1;
            currentTick = -1;
        } else {
            currentScore = currentScore - 2 * resetCount;
            resetCount++;
        }
        toGoUsers.clear();
        usersJustEntered.clear();
        currentDirection = Direction.UP;
    }

    public int getCurrentScore() {
        return currentScore;
    }

}
