package sample.service;

import com.google.gson.Gson;
import sample.domain.ChatType;
import sample.domain.FileOffer;
import sample.domain.Message;
import sample.domain.User;
import sample.net.IMessageSender;
import sample.proto.MessageDTO;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Map;



public class ServerFileService implements IServerFileService {
    //    private Socket socket;
    private final IMessageSender messageSender;
    private final Map<String, FileOffer> pendingFiles;

    public ServerFileService(IMessageSender messageSender, Map<String, FileOffer> pendingFiles) {
        this.messageSender = messageSender;
        this.pendingFiles = pendingFiles;
    }

    @Override
    public String[] offerFile(Message message, String[] parts) {
        String sender = parts[0];
        String timestamp = parts[1];
        String filename = parts[3];
        long fileSize = Long.parseLong(parts[4]);
        String recipient = message.recipient();

        pendingFiles.put(recipient, new FileOffer(sender, recipient, filename, fileSize));
        String returnMessage = (sender + " vil sende dig filen '" + filename + "' (Størrelse: " + fileSize + " bytes. Svar /FILE_ACCEPT eller /FILE_REJECT.");
        return new String[]{returnMessage, recipient};
    }
    @Override
    public void rejectFile(User user, PrintWriter out) {
        FileOffer offer = pendingFiles.remove(user.getUsername());
        if (offer != null) {
            messageSender.unicast("Bruger " + user.getUsername() + " har afvist din fil: " + offer.fileName, offer.sender);
        } else {
            user.getOut().println("Ingen ventende filer");
        }
    }

    @Override
    public void acceptFile(User user, PrintWriter out) {
        FileOffer offer = pendingFiles.remove(user.getUsername());
        if (offer == null) {
            out.println("Ingen ventende filer");
            return;
        }

        int uploadPort, downloadPort;
        try (ServerSocket tmp = new ServerSocket(0);
             ServerSocket tmp2 = new ServerSocket(0)) {
            uploadPort = tmp.getLocalPort();
            downloadPort = tmp2.getLocalPort();
        } catch (IOException e) {
            out.println("Kunne ikke allokere port til filtransfer");
            e.printStackTrace();
            return;
        }


        // Nu er filePort garanteret initialiseret
        MessageDTO toSender = getDtoToSender(uploadPort, offer);
        MessageDTO toReceiver = getDtoToReceiver(downloadPort, offer);
        messageSender.unicast(new Gson().toJson(toSender), offer.sender);
        messageSender.unicast(new Gson().toJson(toReceiver), offer.recipient);

        new Thread(() -> {
            String directoryPath = "server_files";
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    System.err.println("Kunne ikke oprette mappen: " + directoryPath);
                    return; // Stop tråden hvis mappen ikke kan oprettes
                }
            }
            try (ServerSocket uploadSrv = new ServerSocket(uploadPort); // Upload og download server socket sørger for at den venter på at serveren er klar til at downloade fil der fra
                 ServerSocket downloadSrv = new ServerSocket(downloadPort)) {
                // Upload fra afsender
                try (
                        Socket uploadSock = uploadSrv.accept();
                        BufferedInputStream bis = new BufferedInputStream(uploadSock.getInputStream());
                        FileOutputStream fos = new FileOutputStream("server_files/" + offer.fileName)
                ) {
                    byte[] buf = new byte[4096];
                    long total = 0;
                    int r;
                    while ((r = bis.read(buf)) != -1 && total < offer.fileSize) {
                        fos.write(buf, 0, r);
                        total += r;
                    }
                }

                // Download til modtager
                try (
                        Socket downloadSock = downloadSrv.accept();
                        BufferedOutputStream bos = new BufferedOutputStream(downloadSock.getOutputStream());
                        FileInputStream fis = new FileInputStream("server_files/" + offer.fileName)
                ) {
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = fis.read(buf)) != -1) {
                        bos.write(buf, 0, r);
                    }
                    bos.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    private MessageDTO getDtoToReceiver(int filePort, FileOffer offer) {
        MessageDTO toReceiver = new MessageDTO(
                "server",
                ChatType.FILE_PORT,
                filePort + "|DOWNLOAD|" + offer.fileName + "|" + offer.fileSize,
                null,
                offer.recipient
        );
        return toReceiver;
    }

    private MessageDTO getDtoToSender(int filePort, FileOffer offer) {
        MessageDTO toSender = new MessageDTO(
                "server",
                ChatType.FILE_PORT,
                filePort + "|UPLOAD",
                null,
                offer.sender
        );
        return toSender;
    }
}
