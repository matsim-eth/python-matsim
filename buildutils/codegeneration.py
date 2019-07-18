
# ####################################################################### #
# project: python-matsim
# codegeneration.py
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


import xml.etree.ElementTree as et
import os
import subprocess
import atexit
import signal
from typing import List, Tuple

import logging
import pkg_resources
import setuptools
import shutil
import tempfile

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
        with open(pkg_resources.resource_filename('buildutils', 'pom_template.xml'), 'r') as template:
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

    def build_and_start_jvm(self, maven_dir: str, code_dir: str, root_package: str,
                            jvm_path=None):
        # Needs to be done here to work with setuptools setup_requires
        # (otherwise imported before that line is even read)
        import jpype
        if jvm_path is None:
            jvm_path= jpype.get_default_jvm_path()
        _logger.debug('generating classpath in {}'.format(maven_dir))

        pom_path = os.path.join(maven_dir, 'pom.xml')

        self._generate_full_pom(pom_path)
        maven_completion = subprocess.run(['mvn', '-DskipTests=true', 'assembly:assembly'],
                                          cwd=maven_dir)

        maven_completion.check_returncode()

        for jar in os.listdir(os.path.join(maven_dir, 'target')):
            if jar.endswith('jar-with-dependencies.jar'):
                full_path = os.path.join(maven_dir, 'target', jar)
                _logger.debug('adding {} to classpath'.format(full_path))
                jpype.addClassPath(full_path)

        jpype.startJVM(jvm_path, "-Djava.class.path=%s" % jpype.getClassPath(), convertStrings=False)

        # TODO: generate pxi files for all classes on classpath. Needs to be done after build
        # - generate python type-hinted classes
        # - store them next to jar
        # - put them on sys.path
        PyiUtils = jpype.JClass('org.matsim.contrib.pythonmatsim.typehints.PyiUtils')

        try:
            PyiUtils.generatePythonWrappers(code_dir, root_package)
        except jpype.JException as e:
            print(e.message())
            print(e.stacktrace())
            raise e

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


class JavaAdapterCodeGenerationCommand(setuptools.Command):
    description = 'generate Python code wrapping Java code'
    user_options = [
        ('additional-repositories=', 'r', 'semicolon-separated (;) additional buildutils repositories in format id:url'),
        ('additional-dependencies=', 'd', 'semicolon-separated (;) additional buildutils dependencies in format group_id:artifact_id:version')
    ]

    def initialize_options(self):
        """Set default values for options."""

        # Each user option must be listed here with their default value.
        self.additional_repositories = ''
        self.additional_dependencies = ''

    def finalize_options(self):
        self._dependencies = self._explode_option_list(self.additional_dependencies)
        self._repositories = self._explode_option_list(self.additional_repositories)

    def _explode_option_list(self, option: str) -> List[Tuple[str]]:
        if len(option) == 0:
            return []

        repos = option.split(';')
        return [tuple(repo.split(':')) for repo in repos]

    def run(self):
        for id, url in self._repositories:
            add_repository(id=id, url=url)

        for group_id, artifact_id, version in self._dependencies:
            # TODO: allow infered group_id and version
            add_dependency(group_id=group_id, artifact_id=artifact_id, version=version)

        shutil.rmtree('javawrappers', ignore_errors=True)

        with tempfile.TemporaryDirectory() as tmp:
            maven_dir = os.path.join(tmp, 'maven')
            java_gen_dir = os.path.join(tmp, 'codegen')
            os.mkdir(maven_dir)
            build_and_start_jvm(maven_dir,
                                java_gen_dir,
                                'javawrappers')

            shutil.move(os.path.join(java_gen_dir, 'javawrappers'), 'javawrappers')

            shutil.rmtree('javaresources', ignore_errors=True)
            os.mkdir('javaresources')
            open(os.path.join('javaresources', '__init__.py'), 'a').close()
            target_dir = os.path.join(maven_dir, 'target')
            for f in os.listdir(target_dir):
                if f.endswith('jar-with-dependencies.jar'):
                    shutil.copy(
                        os.path.join(target_dir, f),
                        'javaresources')

