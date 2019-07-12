
# ####################################################################### #
# project: python-matsim
# events.py
#                                                                         #
# ####################################################################### #
#                                                                         #
# copyright       : (C) 2019 by the members listed in the COPYING,        #
#                   LICENSE and WARRANTY file.                            #
#                                                                         #
# ####################################################################### #
#                                                                         #
#   This program is free software; you can redistribute it and/or modify  #
#   it under the terms of the GNU General Public License as published by  #
#   the Free Software Foundation; either version 2 of the License, or     #
#   (at your option) any later version.                                   #
#   See also COPYING, LICENSE and WARRANTY file                           #
#                                                                         #
# ####################################################################### #/

 
/* *********************************************************************** *
 * project: python-matsim
 * events.py
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 import logging
import jpype as jp

from collections import defaultdict
import inspect

from EventBuffer_pb2 import EventBuffer
# for the side effect of re-exporting
import events_pb2 as event_type

# this assumes the JVM is started with the correct classpath

_org = jp.JPackage('org')


BufferedProtocolBufferSender = _org.matsim.contrib.pythonmatsim.events.BufferedProtocolBufferSender

logger = logging.getLogger(__name__)


_EVENT_CLASS_TO_TYPE = {
    event_type.ActivityEndEvent: 'actEnd',
    event_type.ActivityStartEvent: 'actStart',
    event_type.LinkEnterEvent : 'linkEnter',
    event_type.LinkLeaveEvent : 'linkLeave',
    event_type.PersonArrivalEvent : 'personArrival',
    event_type.PersonDepartureEvent : 'personDeparture',
    event_type.PersonEntersVehicleEvent : 'personEntersVehicle',
    event_type.PersonLeavesVehicleEvent : 'personLeavesVehicle',
    event_type.PersonMoneyEvent : 'personMoney',
    event_type.PersonStuckEvent : 'personStuck',
    event_type.TransitDriverStartsEvent : 'transitDriverStarts',
    event_type.VehicleAbortsEvent : 'vehicleAborts',
    event_type.VehicleEntersTrafficEvent : 'vehicleEntersTraffic',
    event_type.VehicleLeavesTrafficEvent : 'vehicleLeavesTraffic',
    event_type.GenericEvent : 'genericEvent',
}


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
            method.listened_events.add(_EVENT_CLASS_TO_TYPE[t])

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