package com.indicium.services;

import java.io.File;

public interface IStorageService
{
    String saveFile(String filePath);
    boolean moveToArchive(String filePath);
    File retrieveFromArchive(String filePath);
}
