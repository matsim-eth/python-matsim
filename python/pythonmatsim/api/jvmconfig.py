import xml.etree.ElementTree as et
import tempfile
import os
import subprocess
import atexit
import signal
import jpype
import logging
import pkg_resources
import sys

_logger = logging.getLogger(__name__)


class JvmConfig:
    def __init__(self):
        self._matsim_version = '12.0-SNAPSHOT'

        self._dependencies = []
        self._repositories = []

    def add_dependency(self,
                       artifact_id: str,
                       version: str = '${matsim.version}',
                       group_id: str = 'org.matsim.contrib',
                       ):
        dependency = et.Element('dependency')
        et.SubElement('groupId', dependency).text = group_id
        et.SubElement('artifactId', dependency).text = artifact_id
        et.SubElement('version', dependency).text = version

        self._dependencies.append('')
        for line in et.tostringlist(dependency, encoding="unicode"):
            self._dependencies.append(line)

    def add_repository(self,
                       id: str,
                       url: str,
                       ):
        repository = et.Element('repository')
        et.SubElement('id', repository).text = id
        et.SubElement('url', repository).text = url

        self._repositories.append('')
        for line in et.tostringlist(repository, encoding="unicode"):
            self._repositories.append(line)

    def _generate_properties(self):
        properties = et.Element('properties')

        et.SubElement(properties, 'matsim.version').text = self._matsim_version

        return et.tostringlist(properties, encoding="unicode")

    def _generate_full_pom(self, pom_file):
        # TODO This might not work with zipped distribution
        # could not get resource_stream nor resource_string to work...
        with open(pkg_resources.resource_filename('maven', 'pom_template.xml'), 'r') as template:
            with open(pom_file, 'w') as out:
                for template_line in template:
                    lines = [template_line]

                    if '<!-- PROPERTIES HERE -->' in template_line:
                        lines = self._generate_properties()
                    elif '<!-- ADDITIONAL DEPENDENCIES HERE -->' in template_line:
                        lines = self._dependencies
                    elif '<!-- ADDITIONAL REPOSITORIES HERE -->' in template_line:
                        lines = self._repositories

                    out.writelines(lines)

    def build_and_start_jvm(self, jvm_path=jpype.get_default_jvm_path()):
        # TODO: avoid to rebuild the same over and over.
        # idea:
        # - define some global cache directory ($HOME/.python-matsim or so)
        # - store jars as <hash of pom.xml>/python-matsim.jar
        #
        # Could also still be a temp file, but not sure they get cleaned up regularly by all OSes...
        # Also need to handle failures or updated snapshots...
        temp_dir = tempfile.TemporaryDirectory()
        _logger.debug('generating classpath in {}'.format(temp_dir.name))

        # Only delete temp directory on system exit.
        # The JVM can only be started once per session by JPype due to limitation of JNI
        _register_exit_handler(temp_dir.cleanup)

        pom_path = os.path.join(temp_dir.name, 'pom.xml')

        self._generate_full_pom(pom_path)
        maven_completion = subprocess.run(['mvn', '-DskipTests=true', 'assembly:assembly'],
                                          cwd=temp_dir.name)

        maven_completion.check_returncode()

        for jar in os.listdir(os.path.join(temp_dir.name, 'target')):
            if jar.endswith('jar-with-dependencies.jar'):
                full_path = os.path.join(temp_dir.name, 'target', jar)
                _logger.debug('adding {} to classpath'.format(full_path))
                jpype.addClassPath(full_path)

        jpype.startJVM(jvm_path, "-Djava.class.path=%s" % jpype.getClassPath())

        # TODO: generate pxi files for all classes on classpath. Needs to be done after build
        # - generate python type-hinted classes
        # - store them next to jar
        # - put them on sys.path
        jpype.JClass('org.matsim.contrib.pythonmatsim.typehints.PyiUtils').generatePyiFiles(temp_dir.name)
        sys.path.append(temp_dir.name)

        _logger.debug('done generating classpath')

def _register_exit_handler(f, *args, **kwargs):
    """
    register the function as a cleanup function even on interrupt signals
    :param f:
    :return:
    """

    def handler():
        return f(*args, **kwargs)

    atexit.register(handler)
    signal.signal(signal.SIGTERM, handler)
    signal.signal(signal.SIGINT, handler)


# Create a "global" instance of the config and provide convenience methods to manipulate it
_config = JvmConfig()

add_dependency = _config.add_dependency
add_repository = _config.add_repository
build_and_start_jvm = _config.build_and_start_jvm
