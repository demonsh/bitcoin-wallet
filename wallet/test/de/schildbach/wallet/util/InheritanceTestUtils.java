package de.schildbach.wallet.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InheritanceTestUtils {
    public static String runShellCommand(String command) {
        return runShellCommand(command, false);
    }

    public static String runShellCommand(String command, boolean logOutput) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        StringBuilder outputString = new StringBuilder();

        try {
            Process process = processBuilder.start();

            BufferedReader output =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader erroutput =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = output.readLine()) != null) {
                outputString.append(line + "\n");
            }

            if (logOutput) {
                System.out.println(outputString.toString());
            }

            while ((line = erroutput.readLine()) != null) {
                System.out.println(line);
                outputString.append(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Command " + command + "exited with error code : " + exitCode + "\n");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            return outputString.toString();
        }
    }

    public static String convertByteToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }
}
