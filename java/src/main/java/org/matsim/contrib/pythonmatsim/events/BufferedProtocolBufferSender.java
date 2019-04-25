package org.matsim.contrib.pythonmatsim.events;

import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.pythonmatsim.protobuf.Event2ProtoEvent;
import org.matsim.contrib.pythonmatsim.protobuf.EventBufferOuterClass;
import org.matsim.contrib.pythonmatsim.protobuf.ProtobufEvents;
import org.matsim.core.events.LastEventOfIteration;
import org.matsim.core.events.LastEventOfSimStep;
import org.matsim.core.events.handler.BasicEventHandler;

public class BufferedProtocolBufferSender implements BasicEventHandler {
    private final int bufferSize;
    private final Listener listener;
    private final EventBufferOuterClass.EventBuffer.Builder bufferBuilder =
            EventBufferOuterClass.EventBuffer.newBuilder();

    public BufferedProtocolBufferSender(int bufferSize, Listener listener) {
        // TODO add configuration of what events to forward. This should be resolved on Python side
        // and passed here as an argument
        this.bufferSize = bufferSize;
        this.listener = listener;
    }

    @Override
    public void reset(int iteration) {
        if (bufferBuilder.getEventCount() > 0) {
            throw new IllegalStateException("buffer was not emptied at end of simulation");
        }
        listener.reset(iteration);
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof LastEventOfSimStep) {
            return;
        }
        if (event instanceof LastEventOfIteration) {
            flush();
            return;
        }

        final ProtobufEvents.Event protoEvent = Event2ProtoEvent.getProtoEvent(event);
        bufferBuilder.addEvent(protoEvent);

        if (bufferBuilder.getEventCount() >= bufferSize) {
            flush();
        }
    }

    private void flush() {
        listener.handleEventBuffer(bufferBuilder.build().toByteArray());
        bufferBuilder.clear();
    }

    public interface Listener {
        void handleEventBuffer(byte[] buffer);
        void reset(int iteration);
    }
}
