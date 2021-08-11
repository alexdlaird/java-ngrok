/*
 * Copyright (c) 2021 Alex Laird
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.alexdlaird.ngrok.process;

import com.github.alexdlaird.ngrok.installer.NgrokInstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NgrokProcess {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Process proc = null;

    private Future<List<String>> future = null;

    // TODO: this entire class is a POC placeholder for simple testing while the API is built out

    public NgrokProcess() {
        final NgrokInstaller ngrokInstaller = new NgrokInstaller();
        ngrokInstaller.install();
    }

    public void start() throws IOException, InterruptedException {
        if (nonNull(proc) && nonNull(future)) {
            return;
        }

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("ngrok", "start", "--none");
        proc = processBuilder.start();
        final ProcessTask task = new ProcessTask(proc.getInputStream());
        future = executorService.submit(task);
        Thread.sleep(2000);
    }

    public void stop() throws InterruptedException {
        if (isNull(proc) || isNull(future)) {
            return;
        }

        future.cancel(true);
        proc.descendants().forEach(ProcessHandle::destroy);
        proc.destroy();
        Thread.sleep(2000);

        proc = null;
        future = null;
    }

    public Process getProc() {
        return proc;
    }

    private static class ProcessTask implements Callable<List<String>> {

        private final InputStream inputStream;

        public ProcessTask(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public List<String> call() {
            return new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.toList());
        }
    }
}
