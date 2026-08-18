package com.adawriter.desktop;

/**
 * Abstraction over clipboard read/write for tray assist actions.
 */
public interface ClipboardGateway {

    String readText();

    void writeText(String text);
}
