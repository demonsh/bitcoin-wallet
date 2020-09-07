package de.schildbach.wallet.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InheritanceTestUtils {
    public static void runShellCommand(String command) {
        runShellCommand(command, true);
    }

    public static void runShellCommand(String command, boolean logOutput) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);

        try {
            Process process = processBuilder.start();

            BufferedReader output =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            BufferedReader erroutput =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            if (logOutput) {
                while ((line = output.readLine()) != null) {
                    System.out.println(line);
                }
            }
            while ((line = erroutput.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
