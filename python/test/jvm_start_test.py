
# ####################################################################### #
# project: python-matsim
# jvm_start_test.py
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


