import jpype
import jpype.imports as jimport

jimport.registerDomain('org')

jpype.addClassPath("../../java/target/python-matsim-1.0-SNAPSHOT-jar-with-dependencies.jar")
jpype.startJVM(jpype.get_default_jvm_path(), "-Djava.class.path=%s" % jpype.getClassPath())

from org.matsim.core.scenario import ScenarioUtils
from org.matsim.core.config import ConfigUtils

from unittest import TestCase

class TestIdAccessors(TestCase):
    pass
