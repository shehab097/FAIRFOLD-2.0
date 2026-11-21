package com.fairfold.common.storage;

/**
 * Abstraction over where resume files are stored. The local implementation
 * writes to a disk folder; a future S3/MinIO implementation would use the same
 * interface.
 */
public interface StorageService {

    /** Persists the given bytes and returns a storage key used to retrieve or delete the file later. */
    String save(byte[] content, String originalFilename);

    /** Loads the stored bytes for the given key. */
    byte[] load(String storageKey);

    /** Deletes the stored file, silently ignoring a missing file. */
    void delete(String storageKey);
}
