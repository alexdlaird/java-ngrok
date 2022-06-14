package com.github.alexdlaird.util;

import org.jutils.jprocesses.JProcess;
import org.jutils.jprocesses.ProcessUtils;

import java.io.IOException;

public class NgrokUtils {

    public static JProcess getRunningNgrokChildProcess() throws IOException, InterruptedException {
        JProcess process = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(500); // Timeout to ensure the process is shown if it was just created
            for (JProcess p : new ProcessUtils().getThisProcess().childProcesses) {
                System.out.println(p.name);
                System.out.println(p.pid);
                System.out.println(p.command);
                if(StringUtils.containsIgnoreCase(p.name, "ngrok")){
                    process = p;
                    break;
                }
            }
        }
        return process;
    }

}
