package org.carlspring.strongbox.services;

import org.carlspring.strongbox.storage.MutableStorage;

import java.io.IOException;

/**
 * @author mtodorov
 */
public interface StorageManagementService
{

    void saveStorage(MutableStorage storage)
            throws IOException;

    void removeStorage(String storageId)
            throws IOException;

}
