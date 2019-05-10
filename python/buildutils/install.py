import os
from typing import List, Tuple

import setuptools.command.install as legacy

from buildutils.codegeneration import add_repository, add_dependency, build_and_start_jvm


# It seems this needs to be named install to work, in particular with install_dependencies (which do not get used
# somewhere else as install)
class install(legacy.install):
    description = 'generate Python code wrapping Java code'
    user_options = [
        ('additional-repositories=', 'r', 'semicolon-separated (;) additional buildutils repositories in format id:url'),
        ('additional-dependencies=', 'd', 'semicolon-separated (;) additional buildutils dependencies in format group_id:artifact_id:version')
    ]

    def initialize_options(self):
        """Set default values for options."""
        super().initialize_options()

        # Each user option must be listed here with their default value.
        self.additional_repositories = ''
        self.additional_dependencies = ''

    def finalize_options(self):
        super().finalize_options()
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

        build_and_start_jvm(os.path.abspath('generated-code/'))

        super().run()
        # legacy.install.run(self)
