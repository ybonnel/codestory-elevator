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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ByUserElevator extends CleverElevator {

    private final int minScoreOnFloor;
    private final int minScoreOnFloorWithOver;
    private final int nbPeopleForOver;

    public ByUserElevator(int minScoreOnFloor, int minScoreOnFloorWithOver, int nbPeopleForOver) {
        this.minScoreOnFloor = minScoreOnFloor;
        this.minScoreOnFloorWithOver = minScoreOnFloorWithOver;
        this.nbPeopleForOver = nbPeopleForOver;
    }

    private int currentTick;
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
                if (score > minScoreOnFloorForCurrentOver()) {
                    return true;
                }
            }
        }
        return false;
    }

    private int estimateScore(int currentFloor, Direction currentDirection, boolean openOnCurrentFloor) {
        int score = 0;
        for (Map.Entry<Integer, LinkedList<User>> usersByFloor : waitingUsers.entrySet()) {
            score += estimateScoreForOneFloor(currentFloor, currentDirection, openOnCurrentFloor, usersByFloor);
        }
        for (Map.Entry<Integer, LinkedList<User>> usersByFloor : toGoUsers.entrySet()) {
            score += estimateScoreForOneFloor(currentFloor, currentDirection, openOnCurrentFloor, usersByFloor);
        }
        return score;
    }

    private int estimateScoreForOneFloor(int currentFloor, Direction currentDirection, boolean openOnCurrentFloor, Map.Entry<Integer, LinkedList<User>> usersByFloor) {
        int score = 0;
        if (currentDirection.floorIsOnDirection(currentFloor, usersByFloor.getKey())) {
            if (currentFloor != usersByFloor.getKey() || openOnCurrentFloor) {
                for (User user : usersByFloor.getValue()) {
                    if (user.getDirectionCalled() == currentDirection
                            || user.getDestinationFloor() != null) {
                        int scoreOfUser = user.esperateScore(currentTick, currentFloor);
                        if (currentFloor == usersByFloor.getKey()) {
                            scoreOfUser = scoreOfUser - 1;
                        } else if (openOnCurrentFloor) {
                            scoreOfUser = scoreOfUser - 2;
                        }
                        if (scoreOfUser > 0) {
                            score += scoreOfUser;
                        }
                    }
                }
            }
        }
        return score > minScoreOnFloorForCurrentOver() ? score : 0;
    }

    private int minScoreOnFloorForCurrentOver() {
        if (getPeopleInsideElevator() > nbPeopleForOver) {
            return minScoreOnFloorWithOver;
        }
        return minScoreOnFloor;
    }

    private boolean hasFloorsToGo() {
        for (LinkedList<User> users : waitingUsers.values()) {
            if (!users.isEmpty()) {
                return true;
            }
        }
        for (LinkedList<User> users : toGoUsers.values()) {
            if (!users.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addCall(int floor, String to) {
        if (!waitingUsers.containsKey(floor)) {
            waitingUsers.put(floor, new LinkedList<User>());
        }
        waitingUsers.get(floor).addLast(new User(floor, currentTick, Direction.valueOf(to)));
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
    }

    @Override
    public void userHasExited() {
        super.userHasExited();
        currentScore += toGoUsers.get(currentFloor).removeFirst().esperateScore(currentTick, currentFloor);
    }

    @Override
    public void reset(String cause, Integer lowerFloor, Integer higherFloor, Integer cabinSize) {
        super.reset(cause, lowerFloor, higherFloor, cabinSize);
        if (cause.startsWith("the elevator is at floor ")) {
            currentScore = 0;
        } else {
            currentScore = currentScore - 10;
        }
        currentTick = 0;
        toGoUsers.clear();
        waitingUsers.clear();
        usersJustEntered.clear();
        currentDirection = Direction.UP;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getMinScoreOnFloor() {
        return minScoreOnFloor;
    }

    public int getMinScoreOnFloorWithOver() {
        return minScoreOnFloorWithOver;
    }

    public int getNbPeopleForOver() {
        return nbPeopleForOver;
    }
}
