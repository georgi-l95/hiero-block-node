// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.persistence.storage.write;

import com.hedera.hapi.block.BlockItemUnparsed;
import com.hedera.hapi.block.stream.output.BlockHeader;
import com.hedera.pbj.runtime.ParseException;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The NoOpBlockWriter class is a stub implementation of the block writer intended for testing purposes only. It is
 * designed to isolate the Producer and Mediator components from storage implementation during testing while still
 * providing metrics and logging for troubleshooting.
 */
public final class NoOpBlockWriter implements BlockWriter<List<BlockItemUnparsed>> {
    /**
     * Constructor.
     */
    private NoOpBlockWriter() {}

    private long currentBlockNumber;

    /**
     * This method creates and returns a new instance of
     * {@link NoOpBlockWriter}.
     *
     * @return a new, fully initialized instance of {@link NoOpBlockWriter}
     */
    public static NoOpBlockWriter newInstance() {
        return new NoOpBlockWriter();
    }

    @NonNull
    @Override
    public Optional<Long> write(@NonNull final List<BlockItemUnparsed> valueToWrite)
            throws IOException, ParseException {

        if (valueToWrite.getFirst().hasBlockHeader()) {
            currentBlockNumber = BlockHeader.PROTOBUF
                    .parse(valueToWrite.getFirst().blockHeader())
                    .number();
        }

        if (valueToWrite.getLast().hasBlockProof()) {
            // Returning the BlockItems triggers a
            // PublishStreamResponse to be sent to the
            // upstream producer.
            return Optional.of(currentBlockNumber);
        } else {
            return Optional.empty();
        }
    }
}
