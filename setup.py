
# ####################################################################### #
# project: python-matsim
# setup.py
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

from setuptools import setup, find_packages, Command
from distutils.command.build import build

from buildutils.codegeneration import JavaAdapterCodeGenerationCommand


with open('README.md') as f:
    long_description = f.read()

# Hack to delay evaluation of "find packages" to after code was generated.
# Our issue here is that there a a lot of generated packages, and in the long run we want those to be influenced
# by command line options (for instance adding jars).
# Caveat: this actually depends on implementation details of `setup` and might break with changes in them
# (for instance if list of packages gets cached in a new list)
class PackageFinder:
    @property
    def find_packages(self):
        return find_packages(
            exclude=(
                'buildutils',
                'java',
                'test',
            )
        )

setup(
    name='pythonmatsim',
    version='0.1.1',
    packages=PackageFinder().find_packages,
    #package_data = {
    #    #'buildutils': '*.xml',
    #    'javaresources': 'python-matsim-instance-1.0-SNAPSHOT-jar-with-dependencies.jar',
    #},
    include_package_data=True,
    # resources need to be accessed as normal file system files
    zip_safe=False,
    url='https://github.com/matsim-eth/python-matsim',
    license='GNU GPL 3.0',
    author='Thibaut Dubernet',
    author_email='thibaut.dubernet@ivt.baug.ethz.ch',
    description='A package to use MATSim from Python',
    long_description=long_description,
    long_description_content_type='text/markdown',
    classifiers=[
        "Development Status :: 3 - Alpha",
        "Programming Language :: Java",
        "Programming Language :: Python :: 3.5",
        "Programming Language :: Python :: 3.6",
        "Programming Language :: Python :: 3.7",
        "License :: OSI Approved :: GNU General Public License v3 or later (GPLv3+)",
        "Operating System :: OS Independent",
    ],
    setup_requires=[
        'numpy>=1.6',
        'JPype1==0.7.0',
    ],
    install_requires=[
        'numpy>=1.6',
        'JPype1==0.7.0',
        'protobuf==3.8.0',
    ],
    # Need type hints
    # Should also work with lower, but needs to be tested
    python_requires='>=3.6',
    cmdclass={
        # Not linked to any other command, on purpose:
        # This way, one can distribute a source distribution without requiring users
        # to run code generation.
        # Otherwise, the source distribution is a mess, because it contains the generated code
        # but not the pythonmatsim directory that is needed.
        # Ideally, one should not need to copy pythonmatsim in generatedcode, but I did not find a way yet...
        'codegen': JavaAdapterCodeGenerationCommand,
    },
)
