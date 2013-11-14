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
package fr.ybonnel;

public class User {

    private final int tickEnterBuilding;
    private final int startFloor;
    private final int destinationFloor;
    private int tickEnterElevator = -1;

    public User(int tickEnterBuilding, int startFloor, int destinationFloor) {
        this.tickEnterBuilding = tickEnterBuilding;
        this.startFloor = startFloor;
        this.destinationFloor = destinationFloor;
    }

    public void enterElevator(int tick) {
        tickEnterElevator = tick;
    }

    public int exitElevatorAndComputeScore(int tick) {
        int score = 20 + 2 + Math.abs(startFloor - destinationFloor) - ((tickEnterElevator - tickEnterBuilding) / 2) - (tick - tickEnterElevator);
        if (score < 0) {
            score = 0;
        }
        return score;
    }

    public boolean isInElevator() {
        return tickEnterElevator != -1;
    }


    public int getDestinationFloor() {
        return destinationFloor;
    }

    public int getStartFloor() {
        return startFloor;
    }
}
