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

public abstract class CleverElevator implements Elevator {

    int currentFloor;
    State currentState;

    protected Command openIfCan() {
        if (isClose()) {
            currentState = State.OPEN;
            return Command.OPEN;
        } else {
            return Command.NOTHING;
        }
    }

    @Override
    public void reset(String cause) {
        currentFloor = 0;
        currentState = State.CLOSE;
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
