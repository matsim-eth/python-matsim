import examples.basic_usage as basic
import pythonmatsim.api.logging
import unittest

class ExampleTest(unittest.TestCase):
    def testBasicExampleRuns(self):
        basic.main()