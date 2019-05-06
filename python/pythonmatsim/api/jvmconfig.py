import xml.etree.ElementTree as et
import tempfile
import os
import shutil
import subprocess
import atexit
import signal
import jpype

class JvmConfig:
    def __init__(self):
        self._matsim_version = '11.0'
        self._protobuf_version = '12.0-SNAPSHOT'

        self._dependencies = []

    def add_dependency(self,
                        artifact_id: str,
                        version: str,
                        group_id: str='org.matsim.contrib',
                        ):
        dependency = et.Element('dependency')
        et.SubElement('groupId', dependency).text = group_id
        et.SubElement('artifactId', dependency).text = artifact_id
        et.SubElement('version', dependency).text = version

        self._dependencies.append('')
        for line in et.tostringlist(dependency):
            self._dependencies.append(line)

    def _generate_properties(self):
        properties = et.Element('properties')

        et.SubElement(properties, 'matsim.version').text = self._matsim_version
        et.SubElement(properties, 'matsim.protobuf.version').text = self._protobuf_version

        return et.tostringlist(properties)

    def _generate_full_pom(self, pom_file):
        with open('maven/pom_template.xml', 'r') as template:
            with open(pom_file, 'w') as out:
                lines = [template.readline()]

                if lines.endswith('<!-- PROPERTIES HERE -->'):
                    lines = self._generate_properties()
                elif lines.endswith('<!-- ADDITIONAL DEPENDENCIES HERE -->'):
                    lines = self._dependencies

                out.writelines(lines)

    def build_and_start_jvm(self, jvm_path=jpype.get_default_jvm_path()):
        temp_dir = tempfile.TemporaryDirectory()

        # Only delete temp directory on system exit.
        # The JVM can only be started once per session by JPype due to limitation of JNI
        _register_exit_handler(temp_dir.close)

        pom_path = os.path.join(temp_dir.name, 'pom.xml')

        self._generate_full_pom(pom_path)
        maven_completion = subprocess.run('mvn', '-DskipTests=true', 'assembly:assembly',
                                          cwd=temp_dir,
                                          text=True)

        maven_completion.check_returncode()

        jpype.addClassPath(os.path.join(temp_dir.name, 'target', 'python-matsim-instance-jar-with-dependencies.jar'))
        jpype.startJVM(jvm_path, jpype.getClassPath())


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
    signal.signal(signal.SIGKILL, handler)
