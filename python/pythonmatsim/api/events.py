import jpype as jp

from collections import defaultdict
import inspect

from EventBuffer_pb2 import EventBuffer

# this assumes the JVM is started with the correct classpath

_org = jp.JPackage('org')


BufferedProtocolBufferSender = _org.matsim.contrib.pythonmatsim.events.BufferedProtocolBufferSender


class EventType:
    actEnd = 'actEnd'
    actStart = 'actStart'
    linkEnter = 'linkEnter'
    linkLeave = 'linkLeave'
    personArrival = 'personArrival'
    personDeparture = 'personDeparture'
    personEntersVehicle = 'personEntersVehicle'
    personLeavesVehicle = 'personLeavesVehicle'
    personMoney = 'personMoney'
    personStuck = 'personStuck'
    transitDriverStarts = 'transitDriverStarts'
    vehicleAborts = 'vehicleAborts'
    vehicleEntersTraffic = 'vehicleEntersTraffic'
    vehicleLeavesTraffic = 'vehicleLeavesTraffic'
    genericEvent = 'genericEvent'


class EventListener:
    def __init__(self):
        self._method_per_type = defaultdict(set)
        for name, val in inspect.getmembers(self, predicate=inspect.ismethod):
            if hasattr(val, 'listened_events'):
                for type in val.listened_events:
                    self._method_per_type[type].add(val)

    def _handle_typed_event(self, event_type, event):
        for method in self._method_per_type[event_type]:
            method(event)


def listen_to(*act_types):
    """
    decorator for listening methods of EventListener subclasses.

    Example usage:
    class MyListener(EventListener):
        @listen_to(EventType.actEnd, EventType.actStart)
        def handleActEndAndStart(self, event):
            pass
    """
    def decorator(method):
        if not hasattr(method, 'listened_events'):
            method.listened_events = set()

        for t in act_types:
            method.listened_events.add(t)

        return method

    return decorator


def create_buffered_event_handler(handler, buffer_size):
    class ProtobufHandler:
        def reset(self, iteration):
            if hasattr(handler, "reset"):
                handler.reset(iteration)

        def handleEventBuffer(self, message):
            buffer = EventBuffer()
            buffer.ParseFromString(message[:])

            for event in buffer.event:
                event_type = event.WhichOneof("event_type")
                handler._handle_typed_event(event_type, getattr(event, event_type))

    impl = jp.JProxy("org.matsim.contrib.pythonmatsim.events.BufferedProtocolBufferSender$Listener", inst=ProtobufHandler())
    return BufferedProtocolBufferSender(buffer_size, impl)