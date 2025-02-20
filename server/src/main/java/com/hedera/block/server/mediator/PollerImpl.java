// SPDX-License-Identifier: Apache-2.0
package com.hedera.block.server.mediator;

import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

public class PollerImpl<V> implements Poller<V> {

    private final EventPoller<V> poller;
    private final BatchedData<V> polledData;
    private final RingBuffer<V> ringBuffer;

    /**
     * Leverage the provided ring buffer and batch size to poll for events.
     *
     * @param ringBuffer the ring buffer to poll events from
     * @param batchSize the size of the batch
     */
    PollerImpl(@NonNull final RingBuffer<V> ringBuffer, final int batchSize) {
        this.ringBuffer = Objects.requireNonNull(ringBuffer);
        this.poller = ringBuffer.newPoller();
        ringBuffer.addGatingSequences(poller.getSequence());
        this.polledData = new BatchedData<>(batchSize);
    }

    @Override
    @NonNull
    public V poll() throws Exception {
        if (polledData.getMsgCount() > 0) {
            return polledData.pollMessage();
        }

        loadNextValues(poller, polledData);
        return polledData.getMsgCount() > 0 ? polledData.pollMessage() : null;
    }

    @Override
    public void unsubscribe() {
        ringBuffer.removeGatingSequence(poller.getSequence());
    }

    private EventPoller.PollState loadNextValues(final EventPoller<V> poller, final BatchedData<V> batch)
            throws Exception {
        return poller.poll((event, sequence, endOfBatch) -> batch.addDataItem(event));
    }

    private static class BatchedData<V> {
        private int msgHighBound;
        private final int capacity;
        private final V[] data;
        private int cursor;

        @SuppressWarnings("unchecked")
        BatchedData(final int size) {
            this.capacity = size;
            data = (V[]) new Object[this.capacity];
        }

        private void clearCount() {
            msgHighBound = 0;
            cursor = 0;
        }

        public int getMsgCount() {
            return msgHighBound - cursor;
        }

        public boolean addDataItem(final V event) throws IndexOutOfBoundsException {
            if (msgHighBound >= capacity) {
                throw new IndexOutOfBoundsException("Attempting to add item to full batch");
            }

            data[msgHighBound++] = event;
            return msgHighBound < capacity;
        }

        public V pollMessage() {
            V event = null;
            if (cursor < msgHighBound) {
                event = data[cursor++];
            }
            if (cursor > 0 && cursor >= msgHighBound) {
                clearCount();
            }
            return event;
        }
    }
}
