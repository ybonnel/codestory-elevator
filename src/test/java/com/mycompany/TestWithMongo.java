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
package com.mycompany;

import com.mongodb.MongoClient;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.ybonnel.simpleweb4j.test.SimpleWeb4jTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Random;

import static com.mycompany.Main.startServer;
import static fr.ybonnel.simpleweb4j.SimpleWeb4j.stop;

/**
 * Helper class in charge of start/stop mongo and SimpleWeb4j.
 */
public class TestWithMongo extends SimpleWeb4jTest {

    private static MongodExecutable mongodExe;
    private static MongodProcess mongodProc;
    private static int portMongo = 0;

    @BeforeClass
    public static void startMongo() throws IOException {
        Random random = new Random();
        portMongo = Integer.getInteger("test.mongo.port", random.nextInt(10000) + 10000);

        MongodStarter runtime = MongodStarter.getDefaultInstance();
        mongodExe = runtime.prepare(new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(portMongo, Network.localhostIsIPv6()))
                .build());
        mongodProc = mongodExe.start();
    }

    @AfterClass
    public static void stopMongo() {
        mongodProc.stop();
        mongodExe.stop();
    }

    @Before
    public void clearMongoAndStartServer() throws IOException {
        // Clear mongo
        MongoClient client = new MongoClient("localhost", portMongo);
        for (String dbName : client.getDatabaseNames()) {
            client.dropDatabase(dbName);
        }

        // Start server
        startServer(getPort(), false, portMongo);
    }

    @After
    public void stopServer() {
        stop();
    }
}
