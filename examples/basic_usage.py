
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

jvm.start_jvm()

import javawrappers.org.matsim.core.config as jconfig
import javawrappers.java.net as jnet
import javawrappers.org.matsim.core.controler as jcontroler

from pythonmatsim.api.events import *
from typing import Union

import tempfile


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

        controler = jcontroler.Controler(config)

        class ShoutListener(EventListener):
            def reset(self, iteration):
                print("########################################################################################")
                print(iteration)

            @listen_to(event_type.ActivityStartEvent, event_type.ActivityEndEvent)
            def handleAct(self, event: Union[event_type.ActivityStartEvent, event_type.ActivityEndEvent]):
                # type hints for protobufs are unfortunately a bit noisy
                print(event.persId)

        controler.addEventHandler(ShoutListener())
        controler.run()


# This line is not strictly necessary, but helps IDEs identify that file as "executable"
if __name__ == "__main__":
    main()
