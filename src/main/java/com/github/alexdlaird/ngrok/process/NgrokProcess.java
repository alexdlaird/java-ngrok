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

import com.github.alexdlaird.exception.NgrokException;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class NgrokProcess {
    private final JavaNgrokConfig javaNgrokConfig;

    private final NgrokInstaller ngrokInstaller;

    // TODO: this entire class is a POC placeholder for simple testing while the API is built out

    private String apiUrl = "http://localhost:4040";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Process process;

    private Future<List<String>> future;

    public NgrokProcess(final JavaNgrokConfig javaNgrokConfig,
                        final NgrokInstaller ngrokInstaller) {
        this.javaNgrokConfig = javaNgrokConfig;
        this.ngrokInstaller = ngrokInstaller;

        if (!Files.exists(javaNgrokConfig.getNgrokPath())) {
            this.ngrokInstaller.installNgrok(javaNgrokConfig.getNgrokPath());
        }
        if (!Files.exists(javaNgrokConfig.getConfigPath())) {
            this.ngrokInstaller.installDefaultConfig(javaNgrokConfig.getConfigPath());
        }
    }

    public void start() {
        if (nonNull(process) && nonNull(future)) {
            return;
        }

        final ProcessBuilder processBuilder = new ProcessBuilder();

        final List<String> command = new ArrayList<>();
        command.add(javaNgrokConfig.getNgrokPath().toString());
        command.add("start");
        command.add("--none");

        if (nonNull(javaNgrokConfig.getConfigPath())) {
            command.add(String.format("--config=%s", javaNgrokConfig.getConfigPath().toString()));
        }
        if (nonNull(javaNgrokConfig.getAuthToken())) {
            command.add(String.format("--authtoken=%s", javaNgrokConfig.getAuthToken()));
        }
        if (nonNull(javaNgrokConfig.getRegion())) {
            command.add(String.format("--region=%s", javaNgrokConfig.getRegion()));
        }

        processBuilder.command(command);
        try {
            process = processBuilder.start();
            final ProcessTask task = new ProcessTask(process.getInputStream());
            future = executorService.submit(task);
            Thread.sleep(2000);
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while starting ngrok.", e);
        }
    }

    public void stop() {
        if (isNull(process) || isNull(future)) {
            return;
        }

        future.cancel(true);
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO: remove
            e.printStackTrace();
        }

        process = null;
        future = null;
    }

    public void setAuthToken(final String authToken) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(javaNgrokConfig.getNgrokPath().toString(), "authtoken", authToken);
        try {
            process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while setting the auth token for ngrok.", e);
        }
    }

    public void update() {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(javaNgrokConfig.getNgrokPath().toString(), "update");
        try {
            process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new NgrokException("An error occurred while trying to update ngrok.", e);
        }
    }

    public String getVersion() {
        // TODO: implement capturing version output
        throw new UnsupportedOperationException();
    }

    public Process getProcess() {
        return process;
    }

    public NgrokInstaller getNgrokInstaller() {
        return ngrokInstaller;
    }

    public String getApiUrl() {
        return apiUrl;
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
