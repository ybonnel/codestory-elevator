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

import fr.ybonnel.services.model.Commands;

public interface Elevators {

    void logState();
    Commands nextCommands();
    void call(int floor, String to);
    void go(int cabin, int floorToGo);
    void userHasEntered(int cabin);
    void userHasExited(int cabin);
    void reset(String cause, int lowerFloor, int higherFloor, int cabinSize, int cabinCount);


}
