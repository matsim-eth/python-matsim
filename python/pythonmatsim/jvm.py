import jpype
import pkg_resources
import logging

_logger = logging.getLogger(__name__)


def start_jvm(jvm_path=jpype.get_default_jvm_path(),
              *additional_classpath):
    if not pkg_resources.resource_exists('javaresources', 'python-matsim-instance-1.0-SNAPSHOT-jar-with-dependencies.jar'):
        raise Exception('could not find jar file')

    python_matsim_jar = pkg_resources.resource_filename('javaresources', 'python-matsim-instance-1.0-SNAPSHOT-jar-with-dependencies.jar')


    jpype.addClassPath(python_matsim_jar)

    for jar in additional_classpath:
        jpype.addClassPath(jar)

    _logger.info('start jvm with classpath {}'.format(jpype.getClassPath()))

    jpype.startJVM(jvm_path, "-Djava.class.path=%s" % jpype.getClassPath(), convertStrings=False)


