package org.matsim.contrib.pythonmatsim.events;

import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.pythonmatsim.protobuf.Event2ProtoEvent;
import org.matsim.contrib.pythonmatsim.protobuf.EventBufferOuterClass;
import org.matsim.contrib.pythonmatsim.protobuf.ProtobufEvents;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.handler.BasicEventHandler;

public class BufferedProtocolBufferSender implements BasicEventHandler, AfterMobsimListener {
    private final int bufferSize;
    private final Listener[] listeners;
    private final EventBufferOuterClass.EventBuffer.Builder bufferBuilder =
            EventBufferOuterClass.EventBuffer.newBuilder();

    public BufferedProtocolBufferSender(int bufferSize, Listener... listener) {
        // TODO add configuration of what events to forward. This should be resolved on Python side
        // and passed here as an argument
        this.bufferSize = bufferSize;
        this.listeners = listener;
    }

    @Override
    public void reset(int iteration) {
        if (bufferBuilder.getEventCount() > 0) {
            throw new IllegalStateException("buffer was not emptied at end of simulation: "+bufferBuilder.getEventList());
        }
        for (Listener listener : listeners) {
            listener.reset(iteration);
        }
    }

    @Override
    public void handleEvent(Event event) {
        final ProtobufEvents.Event protoEvent = Event2ProtoEvent.getProtoEvent(event);
        bufferBuilder.addEvent(protoEvent);

        if (bufferBuilder.getEventCount() >= bufferSize) {
            flush();
        }
    }

    public void flush() {
        byte[] message = bufferBuilder.build().toByteArray();
        for (Listener listener : listeners) {
            listener.handleEventBuffer(message);
        }
        bufferBuilder.clear();
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        flush();
    }

    public interface Listener {
        void handleEventBuffer(byte[] buffer);
        void reset(int iteration);
    }
}
