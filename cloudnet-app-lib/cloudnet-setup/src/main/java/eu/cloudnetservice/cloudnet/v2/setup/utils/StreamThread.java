/*
 * Copyright 2017 Tarek Hosni El Alaoui
 * Copyright 2020 CloudNetService
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.v2.setup.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public final class StreamThread implements Runnable {

    private final CountDownLatch countDownLatch;
    private final BufferedReader reader;

    public StreamThread(CountDownLatch countDownLatch, InputStream inputStream) {
        this.countDownLatch = countDownLatch;
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

    }

    @Override
    public void run() {
        try (BufferedReader reader = this.reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            this.countDownLatch.countDown();
        }
    }
}
