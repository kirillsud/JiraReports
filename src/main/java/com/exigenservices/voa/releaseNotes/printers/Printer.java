package com.exigenservices.voa.releaseNotes.printers;

import com.exigenservices.voa.releaseNotes.ReleaseNotes;

import java.io.InputStream;

public interface Printer {
    public boolean print(InputStream inputStream, ReleaseNotes notes);
}
