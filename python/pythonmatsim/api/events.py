import logging
import jpype as jp

from collections import defaultdict
import inspect

from EventBuffer_pb2 import EventBuffer

# this assumes the JVM is started with the correct classpath

_org = jp.JPackage('org')


BufferedProtocolBufferSender = _org.matsim.contrib.pythonmatsim.events.BufferedProtocolBufferSender

logger = logging.getLogger(__name__)


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




@jp.JImplements(BufferedProtocolBufferSender.Listener)
class EventListener:
    @jp.JOverride
    def reset(self, iteration):
        pass

    @jp.JOverride
    def handleEventBuffer(self, message):
        if (len(message) == 0):
            # 0-length message (happening for instance after mobsim) creates problems, because JPype does not have a
            # numpy array to return and spits a list instead. Abort early.
            return

        buffer = EventBuffer()
        buffer.ParseFromString(message[:])

        for event in buffer.event:
            event_type = event.WhichOneof("event_type")
            self._handle_typed_event(event_type, getattr(event, event_type))

    def _method_for_type(self, event_type):
        if not hasattr(self, '_method_per_type'):
            self._create_method_per_type()
        return self._method_per_type[event_type]

    def _create_method_per_type(self):
        logger.info('Constructing listener list')
        self._method_per_type = defaultdict(set)
        for name, val in inspect.getmembers(self, predicate=inspect.ismethod):
            if hasattr(val, 'listened_events'):
                for type in val.listened_events:
                    self._method_per_type[type].add(val)
                logger.info('Method {} listens to the following event types: {}'.format(name, val.listened_events))

    def _handle_typed_event(self, event_type, event):
        for method in self._method_for_type(event_type):
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

@jp.JImplementationFor("org.matsim.core.controler.Controler")
class _ControlerEventListenerCustomizer:
    # TODO: find a way to have this land in the stubs files...
    def addEventHandler(self, handler, buffer_size=1):
        # Somehow does not seem to work...
        #if isinstance(handler, BufferedProtocolBufferSender.Listener):
        if isinstance(handler, EventListener):
            add_event_handler(self, handler, buffer_size)
        else:
            self.getEvents().addHandler(handler)

def add_event_handler(controler, handler, buffer_size=1):
    wrapped = BufferedProtocolBufferSender(buffer_size, handler)
    controler.getEvents().addHandler(wrapped)
    controler.addControlerListener(wrapped)