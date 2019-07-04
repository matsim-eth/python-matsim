import pythonmatsim.jvm as jvm

jvm.start_jvm()

import javawrappers.org.matsim.core.config as jconfig
import javawrappers.org.matsim.core.controler as jcontroler

# in PyCharm, such a long chain is possible with autocomplete all the way
jconfig.ConfigUtils.createConfig().global_().setCoordinateSystem('epsg:1234')

