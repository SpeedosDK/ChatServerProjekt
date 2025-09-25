package sample.service;

import sample.proto.MessageDTO;

import java.io.*;
import java.net.Socket;

public class ClientFileService implements IFileTransferService{
    private Socket socket;
    private static final int BAR_WITDH = 50;

    @Override
    public void fileTransfer(MessageDTO msg, String pendingFileName, long pendingFileSize, String host) {
        String[] parts = msg.payload().split("\\|");//Deler ved hver pipe (eller hvad det nu hedder)
        int    filePort = Integer.parseInt(parts[0]);
        String role     = parts[1];

        if (role.equals("UPLOAD")) {
            // Afsender skal uploade til serveren
            Thread uploadThread = new Thread(() ->
                    sendFileOnSeparateSocket(
                            "files/" + pendingFileName,
                            host,
                            filePort
                    )
            );
            uploadThread.start();
            try {
                uploadThread.join();
            } catch (InterruptedException e) {
                System.out.println("Fejl i upload: " + e);
            }

        } else if (role.equals("DOWNLOAD")) {

            // Modtager skal downloade fra serveren
            String fileName = parts[2];
            long   fileSize = Long.parseLong(parts[3]);

            Thread downloadThread = new Thread(() ->
                    receiveFileOnSeparateSocket(
                            "received_files/" + fileName,
                            host,
                            filePort,
                            fileSize
                    )
            );
            downloadThread.start();
        }
    }


    private void sendFileOnSeparateSocket(String filePath, String host, int port) {
        File file = new File(filePath);
        System.out.println("Sender fil via separat socket " + port + ": " + file.getName());
        try (
                Socket fs = new Socket(host, port);
                BufferedOutputStream bos = new BufferedOutputStream(fs.getOutputStream());
                BufferedInputStream  bis = new BufferedInputStream(new FileInputStream(file))
        ) {
            byte[] buffer = new byte[4096];
            int    r;
            while ((r = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, r);
            }
            bos.flush();
            System.out.println("Fil sendt: " + file.getName());
        } catch (IOException e) {
            System.out.println("Fejl ved filtransfer på port " + port + ": " + e.getMessage());
        }
    }

    private void receiveFileOnSeparateSocket(String savePath,
                                             String host,
                                             int port,
                                             long fileSize) {
        try {
            Thread.sleep(1000); // 1 sekund forsinkelse
        } catch (InterruptedException ignored) {}

        try (
                Socket sock = new Socket(host, port);
                BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
                FileOutputStream     fos = new FileOutputStream(savePath)
        ) {
            System.out.println("Downloader fil fra server…");
            byte[] buffer = new byte[4096];
            long   total  = 0;
            int lastPercent = -1;
            long start = System.nanoTime();

            while (total < fileSize) {
                int toRead = (int) Math.min(buffer.length, fileSize - total);
                int read   = bis.read(buffer, 0, toRead);
                if (read < 0) break;
                fos.write(buffer, 0, read);
                try {
                    Thread.sleep(1);//For Caspers progress bar demo :-)
                } catch (InterruptedException ignored) {}
                total += read;

                if (read > 0) {
                    int percent =(int)((total * 100) / fileSize);
                    if (percent != lastPercent) {
                        printProgressBar(percent, total, fileSize, start);
                    }
                }
            }
            double elapsed = (System.nanoTime() - start) / 1_000_000_000.0;
            System.out.printf("\nFil gemt som: %s (tid: %.2fs)\n", savePath, elapsed);

        } catch (IOException e) {
            System.err.println("Fejl ved download: " + e.getMessage());
        }
    }

    private static void printProgressBar(int percent, long received, long total, long startNano) {
        int filled = (int)((percent / 100.0) * BAR_WITDH);
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < BAR_WITDH; i++) {
            bar.append(i < filled ? '=':' ');
        }
        bar.append("]");
        String eta = etaString(received, total, startNano);

        System.out.print("\r" + bar + " " + percent + "% " + eta);
        System.out.flush();
    }

    private static String etaString(long received, long total, long startNano) {
        if (received >= total) return "eta: 0.0s";
        if (received <= 0) return "eta: --";
        long now = System.nanoTime();
        double elapsed = (now - startNano) / 1_000_000_000.0; // sek
        double rate = received / elapsed; // bytes/sek
        long remaining = total - received;
        double eta = remaining / Math.max(1.0, rate);
        return String.format("eta: %.1fs", eta);
    }
}
