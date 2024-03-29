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
package fr.ybonnel.services.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class User {

    private final int startFloor;

    private final int startTick;

    private final Direction directionCalled;


    private Integer destinationFloor;
    private Integer enterElevatorTick;

    public User(int startFloor, int startTick, Direction directionCalled) {
        this.startFloor = startFloor;
        this.startTick = startTick;
        this.directionCalled = directionCalled;
    }

    public void go(int destinationFloor, int enterElevatorTick) {
        this.destinationFloor = destinationFloor;
        this.enterElevatorTick = enterElevatorTick;
    }

    public int esperateScore(int currentTick, int currentFloor) {
        int baseScore = 20;

        int neededTicks = destinationFloor == null ? 0
                : Math.abs(destinationFloor - startFloor) + 2;

        int waitTime = enterElevatorTick == null
                ? currentTick - startTick
                : enterElevatorTick - startTick;

        int travelTime = enterElevatorTick == null ? 0
                : currentTick - enterElevatorTick;

        int neededTicksWithCurrentFloor = destinationFloor == null
                ? Math.abs(currentFloor - startFloor)
                : Math.abs(currentFloor - destinationFloor);


        int score = baseScore + neededTicks - (waitTime/2) - travelTime - neededTicksWithCurrentFloor;
        if (score < 0) {
            score = 0;
        }
        return score;
    }

    public Direction getDirectionCalled() {
        return directionCalled;
    }

    public Integer getDestinationFloor() {
        return destinationFloor;
    }

    public String state(int currentTick, int floor) {
        StringBuilder builder = new StringBuilder("User(");
        builder.append(esperateScore(currentTick, floor));
        if (destinationFloor == null) {
            builder.append(',').append(directionCalled);
        }
        builder.append(')');
        return builder.toString();
    }

    public static List<String> userToState(Collection<User> users, int currentTick, int currentFloor) {
        List<String> results = new ArrayList<>();
        for (User user : users) {
            results.add(user.state(currentTick, currentFloor));
        }
        return results;
    }

    public int getStartFloor() {
        return startFloor;
    }
}
