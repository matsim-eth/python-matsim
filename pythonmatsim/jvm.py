
# ####################################################################### #
# project: python-matsim
# jvm.py
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

import jpype
import pkg_resources
import logging

_logger = logging.getLogger(__name__)


def start_jvm(jvm_path=jpype.get_default_jvm_path(),
              *additional_classpath):
    if not pkg_resources.resource_exists('javaresources', 'python-matsim-instance-1.0-SNAPSHOT-jar-with-dependencies.jar'):
        raise RuntimeError('could not find jar file')

    if jpype.isJVMStarted():
        # TODO: check that classpath etc. are the ones we want
        _logger.info("JVM is already live, do nothing.")
        return

    python_matsim_jar = pkg_resources.resource_filename('javaresources', 'python-matsim-instance-1.0-SNAPSHOT-jar-with-dependencies.jar')

    jpype.addClassPath(python_matsim_jar)

    for jar in additional_classpath:
        jpype.addClassPath(jar)

    _logger.info('start jvm with classpath {}'.format(jpype.getClassPath()))

    jpype.startJVM(jvm_path, "-Djava.class.path=%s" % jpype.getClassPath(), convertStrings=False)



