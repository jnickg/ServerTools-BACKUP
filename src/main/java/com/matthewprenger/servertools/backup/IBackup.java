package com.matthewprenger.servertools.backup;

import java.io.File;

public interface IBackup extends Runnable {

    public File getSourceDir();

    public File getDestinationFile();
}
