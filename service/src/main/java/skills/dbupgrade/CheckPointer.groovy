package skills.dbupgrade


import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class CheckPointer implements AutoCloseable {

    static final String CHECKPOINT_FILE_EXT = "checkpoint"

    Path checkPointFile
    int startAt = 0
    int lastRecorded = 0
    FileChannel checkpointWriter

    public CheckPointer(Path parentDir, String filename) {
        checkPointFile = parentDir.resolve("${filename}.${CHECKPOINT_FILE_EXT}")
        if (Files.isReadable(checkPointFile)) {
            String asString = Files.readString(checkPointFile, StandardCharsets.UTF_8)
            startAt = Integer.valueOf(asString)
        }

        checkpointWriter = FileChannel.open(checkPointFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }

    int getLastReadRecord() {
        return startAt
    }

    void recordRecord(int recordNum) {
        Integer asInteger = Integer.valueOf(recordNum)
        String asString = asInteger.toString()
        checkpointWriter.write(ByteBuffer.wrap(asString.getBytes(StandardCharsets.UTF_8)), 0)
        checkpointWriter.force(false)
        lastRecorded = recordNum
    }

    void cleanup() {
        checkpointWriter?.close()
        checkpointWriter = null
        Files.delete(checkPointFile)
    }

    @Override
    void close() throws Exception {
        checkpointWriter?.close()
        checkpointWriter = null
    }
}
