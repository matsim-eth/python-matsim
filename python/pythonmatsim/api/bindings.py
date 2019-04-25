from jpype import *

# This assumes the JVM is already started

_org = JPackage('org')

Controler = _org.matsim.core.controler.Controler
Scenario = _org.matsim.core.scenario.Scenario
Config = _org.matsim.core.config.Config
