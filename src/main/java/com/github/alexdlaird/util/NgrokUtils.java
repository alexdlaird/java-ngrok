package com.github.alexdlaird.util;

import org.jutils.jprocesses.JProcess;
import org.jutils.jprocesses.ProcessUtils;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class NgrokUtils {

    /**
     * Returns the currently running ngrok child process.
     * @param timeCreated timestamp in milliseconds. Must not be 100% exact. Only relevant
     *                              when there are multiple ngrok
     *                              child processes running at the same time.
     *                              If there are, the process closer to this time
     *                              aka with a smaller difference will be returned.
     */
    public static JProcess getRunningNgrokChildProcess(long timeCreated) throws IOException, InterruptedException, ParseException {

        // Fetch child-processes that contain ngrok in their file name
        // and add them to the list below.
        List<JProcess> list = new ArrayList<>(1);
        for (int i = 0; i < 10; i++) {
            Thread.sleep(500); // Timeout to ensure the process is shown if it was just created
            for (JProcess p : new ProcessUtils().getThisProcess().childProcesses) {
                //System.out.println(p.toPrintString() + p.getTimestampStart());
                if(StringUtils.containsIgnoreCase(p.name, "ngrok")){
                    //System.out.println("FOUND: "+p.toPrintString()+ p.getTimestampStart());
                    list.add(p);
                }
            }
            //System.out.println();
            if(!list.isEmpty()) break;
        }

        if(list.size() == 1) return list.get(0);

        // Compare processes and see
        // which processes create time is
        // nearer to the expected timeCreated
        JProcess process = null;
        long smallestResult = Long.MAX_VALUE;
        for (JProcess p : list) {
            long result = Math.abs(timeCreated - p.getTimestampStart().getTime());
            if(result <= smallestResult){
                smallestResult = result;
                process = p;
            }
        }
        return process;
    }

}
