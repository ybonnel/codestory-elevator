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

import com.github.kevinsawicki.http.HttpRequest;
import fr.ybonnel.services.Elevator;
import org.junit.Test;

public class RegisterUser {

    public static class User {
        private String pseudo;
        private String email;
        private String serverUrl;

        public User(String pseudo, String email, String serverUrl) {
            this.pseudo = pseudo;
            this.email = email;
            this.serverUrl = serverUrl;
        }

        public void register() {
            System.out.println(
                    HttpRequest.post("http://localhost:8080/resources/player/register?" +
                            "email=" + email +
                            "&pseudo=" + pseudo +
                            "&serverURL=" + serverUrl).body());
        }
    }

    @Test
    public void registerUsers() {

        for (Elevator elevator : Main.elevators) {
            String name = elevator.getClass().getSimpleName();
            new User(name, name + "@gmail.com", "http://localhost:9999/" + name + "/").register();
        }
    }


}
