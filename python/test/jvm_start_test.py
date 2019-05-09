import unittest
import jpype
import buildutils.codegeneration as jc


class JvmStartupTest(unittest.TestCase):
    def testStartupDoesNotFail(self):
        try:
            jc.build_and_start_jvm()
        except jpype.JavaException as exception:
            print(exception.message())
            print(exception.stacktrace())
            self.fail()


