package sample.service;

import sample.proto.MessageDTO;

public interface IFileTransferService {
    void fileTransfer(MessageDTO msg, String pendingFileName, long pendingFileSize, String host);
}
