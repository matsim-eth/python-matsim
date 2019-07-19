
# ####################################################################### #
# project: python-matsim
# basic_usage.py
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


import pythonmatsim.jvm as jvm
import numpy as np

jvm.start_jvm()

import javawrappers.org.matsim.core.config as jconfig
import javawrappers.java.net as jnet
import javawrappers.org.matsim.core.controler as jcontroler
import javawrappers.org.matsim.core.scenario as jscenario
import javawrappers.org.matsim.api.core.v01.network as jnetapi
import javawrappers.org.matsim.api.core.v01.population as jpopapi

from pythonmatsim.api.events import *
from typing import Union

import tempfile
import jpype

def main():
    # Do everything in a temporary directory that will be deleted at the end. Not always the best choice,
    # but good idea when doing interactive analysis
    with tempfile.TemporaryDirectory() as tmp:
        # in PyCharm, such chains are possible with autocomplete all the way, with type hints for the parameters
        # This unfortunately does not work in Jupyter.
        config = jconfig.ConfigUtils.loadConfig(jnet.URL('https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/equil/config_plans1.xml'))

        config.controler().setDumpDataAtEnd(False)
        config.controler().setLastIteration(1)

        config.controler().setOutputDirectory(tmp)

        scenario = jscenario.ScenarioUtils.loadScenario(config)

        # You can manipulate the scenario in any way you like
        # Type hints from Java generics are not yet supported, so if you want support, you need explicit hints
        link: jnetapi.Link
        for link in scenario.getNetwork().getLinks().values():
            link.setCapacity(link.getCapacity() * 2)

        person: jpopapi.Person
        for person in scenario.getPopulation().getPersons().values():
            plan: jpopapi.Plan
            for plan in person.getPlans():
                # JPype is usually rather smart at deducting the Java type from python primitive types, but there
                # are some caveats. Here, for instance, the setScore method expects a Double object, but np.nan
                # is a float that can only be converted to primitive types, such as double.
                # jpype.JObject converts it to the "boxed" type.
                plan.setScore(jpype.JObject(np.nan))

        controler = jcontroler.Controler(scenario)

        class ShoutListener(EventListener):
            def reset(self, iteration):
                print("########################################################################################")
                print(iteration)

            @listen_to(event_type.ActivityStartEvent, event_type.ActivityEndEvent)
            def handleAct(self, event: Union[event_type.ActivityStartEvent, event_type.ActivityEndEvent]):
                # type hints for protobufs are unfortunately a bit noisy
                # for the moment, the structure of the events is similar to the MATSim ones,
                # but in a future version, it is planned to make them richer
                print(event.persId)

        controler.addEventHandler(ShoutListener())
        controler.run()


if __name__ == "__main__":
    main()
